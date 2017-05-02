import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;


public class MapServerHttpServlet extends HttpServlet {
	private Connection conn;
	private Statement st;
	private ResultSet rs=null;
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
//		try {
//			Context context = new InitialContext();  
//			DataSource ds = (DataSource)context.lookup("java:/comp/env/jdbc/oracleds");  
//			conn = ds.getConnection();  
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		try {
//			Class.forName("oracle.jdbc.driver.OracleDriver");
//			conn = DriverManager.getConnection(
//					"jdbc:oracle:thin:@localhost:1521:orcl","scott","My215909");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		//mysql
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/map", "mymap", "mymap");
			System.out.println("getConnection");
		} catch (Exception e) {
			e.printStackTrace();
		}
		DataInputStream dis=new DataInputStream(req.getInputStream());
		DataOutputStream dos=new DataOutputStream(resp.getOutputStream());
		int ms=dis.readInt();
		System.out.println(ms);
		switch(ms){
			case MyProtocal.LOGIN:
				login(dis, dos);
				break;
			case MyProtocal.STUDENT_REGISTER:
				studentRegister(dis, dos);
				break;
			case MyProtocal.TEACHER_REGISTER:
				teacherRegister(dis, dos);
				break;
			case MyProtocal.UPDATA_ADDRESS:
				updateAddress(dis, dos);
				break;
			case MyProtocal.GET_CLASSMATE_DATA:
				getClassMataList(dis, dos);
				break;
			case MyProtocal.GET_TEACHER_DATA:
				getTeacherData(dis, dos);
				break;
			case MyProtocal.GET_STUDENT_LOCATION:
				getStudentLocation(dis, dos);
				break;
			case MyProtocal.GET_MSG_LIST:
				getMsgList(dis, dos);
				break;
			case MyProtocal.ADD_MSG:
				addMsg(dis, dos);
				break;
		}
		dis.close();
		dos.close();
		
		super.doPost(req, resp);
	}
	private void addMsg(DataInputStream dis, DataOutputStream dos){
		try {
			String tno = dis.readUTF();
			String msgTitle = dis.readUTF();
			String msgCtx = dis.readUTF();
			String time = dis.readUTF();
			long createTime= dis.readLong();
			String sql = "INSERT INTO MSGINFO " +
					"VALUES('"+msgTitle+"','"+msgCtx+"','"+time+"','"+tno+"',"+createTime+")";
			st = conn.createStatement();
			int n = st.executeUpdate(sql);
			if(n==1){
				dos.writeInt(MyProtocal.OK);
				dos.flush();
			}else {
				dos.writeInt(MyProtocal.ERROR);
				dos.flush();
			}
			st.clearBatch();
			st.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	private void getMsgList(DataInputStream dis, DataOutputStream dos){
		try {
			String sno = dis.readUTF();
			String msgListStr = "";
			String sql = "SELECT MSG_TITLE,MSG_TIME,MSG_CTX FROM MSGINFO WHERE TNO = " +
								"(SELECT TNO FROM STUDENT WHERE SNO = '"+sno+"') " +
										"ORDER BY MSG_CREATETIME DESC ";//从学生地图传来的其实是SNO
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			while(rs.next()){
				msgListStr=msgListStr+
					rs.getString("MSG_TITLE")+"\n\n"+
					"                           "+rs.getString("MSG_TIME")+","+
					rs.getString("MSG_CTX")+"#";
			}
			System.out.println("msgListStr:"+msgListStr);
			if(!(msgListStr.equals("")||msgListStr.isEmpty())){
				dos.writeInt(MyProtocal.OK);
				dos.writeUTF(msgListStr);
				dos.flush();
			}else {
				dos.writeInt(MyProtocal.ERROR);
				dos.flush();
			}
			st.clearBatch();
			st.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	private void getStudentLocation(DataInputStream dis, DataOutputStream dos){
		try {
			String tno = dis.readUTF();
			String studentLocStr = "";
			String sql = "SELECT SNAME,SLATITUDE,SLONGITUDE FROM STUDENT WHERE TNO = "+tno;
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			while(rs.next()){
				studentLocStr=studentLocStr+
					rs.getString("SLATITUDE")+","+
					rs.getString("SLONGITUDE")+","+
					rs.getString("SNAME")+"-";
			}
			if(!(studentLocStr.equals("")||studentLocStr.isEmpty())){
				dos.writeInt(MyProtocal.OK);
				dos.writeUTF(studentLocStr);
				dos.flush();
			}else {
				dos.writeInt(MyProtocal.ERROR);
				dos.flush();
			}
			st.clearBatch();
			st.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	private void getTeacherData(DataInputStream dis, DataOutputStream dos){
		try {
			String teacherStr = "";
			st = conn.createStatement();
			rs = st.executeQuery("SELECT TNAME FROM TEACHER");
			while(rs.next()){
				teacherStr=teacherStr+rs.getString("TNAME")+"-";
			}
			if(!(teacherStr.equals("")||teacherStr.isEmpty())){
				dos.writeInt(MyProtocal.OK);
				dos.writeUTF(teacherStr);
				dos.flush();
			}else {
				dos.writeInt(MyProtocal.ERROR);
				dos.flush();
			}
			st.clearBatch();
			st.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	private void getClassMataList(DataInputStream dis, DataOutputStream dos){
		try {
			int id = dis.readInt();
			String tno = dis.readUTF();
			String classMateStr = "";
			String sql = "SELECT SNAME,SNO,STEL,SADDRESS FROM STUDENT WHERE TNO = "+tno;
			if(id==MyProtocal.ID_STUDENT){
				sql = "SELECT SNAME,SNO,STEL,SADDRESS FROM STUDENT WHERE TNO = " +
						"(SELECT TNO FROM STUDENT WHERE SNO = '"+tno+"')";//从学生地图传来的其实是SNO
			}
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			if(id==MyProtocal.ID_STUDENT){
				while(rs.next()){
					classMateStr=classMateStr+
						"姓名："+rs.getString("SNAME")+"\n"+
						"学号："+rs.getString("SNO")+"\n"+
						"电话："+rs.getString("STEL")+"-";
				}
			}else{
				while(rs.next()){
					classMateStr=classMateStr+
						"姓名："+rs.getString("SNAME")+"\n"+
						"学号："+rs.getString("SNO")+"\n"+
						"电话："+rs.getString("STEL")+"\n"+
						"地址："+rs.getString("SADDRESS")+"-";
				}
			}
			if(!(classMateStr.equals("")||classMateStr.isEmpty())){
				dos.writeInt(MyProtocal.OK);
				dos.writeUTF(classMateStr);
				dos.flush();
			}else {
				dos.writeInt(MyProtocal.ERROR);
				dos.flush();
			}
			st.clearBatch();
			st.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	private void updateAddress(DataInputStream dis, DataOutputStream dos) {//更新地址
		try {
			String no = dis.readUTF();
			String latitude = dis.readUTF();
			String longitude = dis.readUTF();
			String address = dis.readUTF();
			System.out.println(latitude+"-"+longitude+"-"+address+"---上传成功!");
			st = conn.createStatement();
			int n = st.executeUpdate("UPDATE STUDENT SET " +
					"SLATITUDE='"+latitude+"'," +
					"SLONGITUDE='"+longitude+"'," +
					"SADDRESS='"+address+"' " +
					"WHERE SNO='"+no+"'");
			System.out.println("n= "+n);
			if(n==1){
				System.out.println(latitude+"-"+longitude+"-"+address+"---上传成功!");
				dos.writeInt(MyProtocal.OK);
				dos.flush();				
			}else{
				dos.writeInt(MyProtocal.ERROR);
				dos.flush();
			}
			st.clearBatch();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}


	private void login(DataInputStream dis,DataOutputStream dos){//登录
		try {
			int id = dis.readInt();
			int no = dis.readInt();
			String password = dis.readUTF();
			System.out.println(id+"-"+no+"-"+password);
			st = conn.createStatement();
			if(id==MyProtocal.ID_STUDENT){
				rs = st.executeQuery(
						"SELECT * FROM STUDENT " +
						"WHERE SNO = "+no+" AND SPWD = '"+password+"'");
			}else if(id==MyProtocal.ID_TEACHER){
				rs = st.executeQuery(
						"SELECT * FROM TEACHER " +
						"WHERE TNO = "+no+" AND TPWD = '"+password+"'");
			}
			if(rs.next()){
				dos.writeInt(MyProtocal.OK);
				dos.flush();
			}else {
				dos.writeInt(MyProtocal.ERROR);
				dos.flush();
			}
			st.clearBatch();
			st.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	private void studentRegister(DataInputStream dis,DataOutputStream dos){//注册
		try {
			int no = dis.readInt();
			String password = dis.readUTF();
			String name = dis.readUTF();
			String tel = dis.readUTF();
			String gender = dis.readUTF();
			String teacherName = dis.readUTF();
			st = conn.createStatement();
			int n=st.executeUpdate("INSERT INTO STUDENT(SNO,SNAME,SPWD,STEL,SGENDER,TNO) " +
					"VALUES('"+no+"','"+name+"','"+password+"',"+tel+",'"+gender+"'," +
							"(SELECT TNO FROM TEACHER WHERE TNAME='"+teacherName+"'))");
			if(n==1){
				dos.writeInt(MyProtocal.OK);
				dos.flush();				
			}else{
				dos.writeInt(MyProtocal.ERROR);
				dos.flush();
			}
			st.clearBatch();
			st.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void teacherRegister(DataInputStream dis,DataOutputStream dos){//注册
		try {
			int no = dis.readInt();
			String password = dis.readUTF();
			String name = dis.readUTF();
			String tel = dis.readUTF();
			String gender = dis.readUTF();
			st = conn.createStatement();
			int	n=st.executeUpdate("INSERT INTO TEACHER(TNAME,TPWD,TNO,TTEL,TGENDER) " +
						"VALUES('"+name+"','"+password+"','"+no+"','"+tel+"','"+gender+"')");
			if(n==1){
				dos.writeInt(MyProtocal.OK);
				dos.flush();				
			}else{
				dos.writeInt(MyProtocal.ERROR);
				dos.flush();
			}
			st.clearBatch();
			st.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}

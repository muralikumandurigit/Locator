package LocatorServer.dblayer.tables;

public class History {
	private String fromUser;
	private String toUser;
	private String Response;

	public String getFromuser(){
		return fromUser;
	}

	public void setFromuser(String fromUser){
		this.fromUser=fromUser;
	}

	public String getTouser(){
		return toUser;
	}

	public void setTouser(String toUser){
		this.toUser=toUser;
	}

	public String getResponse(){
		return Response;
	}

	public void setResponse(String Response){
		this.Response=Response;
	}

}

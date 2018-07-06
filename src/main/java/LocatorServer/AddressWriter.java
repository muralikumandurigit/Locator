package LocatorServer;

import java.io.IOException;



public class AddressWriter implements Runnable {

	MainClass obj;
	
	public AddressWriter(MainClass mainObj) {
		obj = mainObj;
		// TODO Auto-generated constructor stub
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(true)
		{
		while(MainClass.address.size() > 0)
		{
			FromVsAddress address = MainClass.address.remove();
			try {
				obj.writeToFileAbtPath(address.getAddress());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
		
	}

}
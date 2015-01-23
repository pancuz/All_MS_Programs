
public class MonitorHeartBeat implements Runnable {

	public void run(){
		
		while(true){
			
			for(int i=0;i<MetaMainClass.listOfServer.size();i++){
				//System.out.println(System.currentTimeMillis()+": "+MetaMainClass.serverObjArray[i].serverId+":\t"+MetaMainClass.serverObjArray[i].status);
				
			//System.out.println((MetaMainClass.mapServerHeartBeat.get(MetaMainClass.listOfServer.get(i)))+ "   ===  ");	
				if(((System.currentTimeMillis()) - (MetaMainClass.mapServerHeartBeat.get(MetaMainClass.listOfServer.get(i)))>15000)&&(MetaMainClass.serverObjArray[i].status)){
					MetaMainClass.serverObjArray[i].status=false;
					System.out.println("\n\n XXX\n === Server has failed === \nXXX" + MetaMainClass.serverObjArray[i].serverId);
					//After server has failed - call serverFailed method with failed server name
				//	MetaMainClass.serverFailed(MetaMainClass.serverObjArray[i].serverId);
					//MetaMainClass.listOfServer.remove(MetaMainClass.serverObjArray[i].serverId);
					//MetaMainClass.hashStoreFreeSpace.put(MetaMainClass.serverObjArray[i].serverId,100.0);
					//MetaMainClass.chunkMap.remove(MetaMainClass.serverObjArray[i].serverId);
					//MetaMainClass.hashStoreFreeSpace.remove(MetaMainClass.serverObjArray[i].serverId);
					MetaMainClass.serverFailed(MetaMainClass.serverObjArray[i].serverId);
				}
				//else{
				//	MetaMainClass.serverObjArray[i].status=true;
				//	MetaMainClass.serverObjArray[i].started=true;
			//	}
			}
			try{
				System.out.println(System.currentTimeMillis()+": Sleeping");
				Thread.sleep(5000);
			}catch(InterruptedException i){
				System.out.println(i);
			}
		}
		
		
	}

	
}

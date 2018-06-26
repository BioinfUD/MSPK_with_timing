
import com.google.common.base.Stopwatch;


import java.io.*;
import java.util.concurrent.CountDownLatch;

public class Partition{
	
	private int k;
	private String inputfile;
	private int numOfBlocks;
	private int pivotLen;
	private int bufSize;
	
	private Object lock = new Object();
	private Object[] writeLocks;
	
	private FileReader frG;
	private BufferedReader bfrG;
	private FileWriter[] fwG;
	private BufferedWriter[] bfwG;
	
	private int readLen;
	
	private static int[] valTable = new int[]{0,-1,1,-1,-1,-1,2,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,3};
	private static char[] twinTable = new char[]{'T','0','G','0','0','0','C','0','0','0','0','0','0','0','0','0','0','0','0','A'};

	public Partition(int kk, String infile, int numberOfBlocks, int pivotLength, int bufferSize, int readLen){
		this.k = kk;
		this.inputfile = infile;
		this.numOfBlocks = numberOfBlocks;
		this.pivotLen = pivotLength;
		this.bufSize = bufferSize;
		this.readLen = readLen;
	}
	
	private boolean isReadLegal(char[] line){
		int Len = line.length;
		for(int i=0; i<Len; i++){
			if(line[i]!='A' && line[i]!='C' && line[i]!='G' && line[i]!='T')
				return false;
		}
		return true;
	}
	
	private int strcmp(char[] a, char[] b, int froma, int fromb, int len){
		for(int i = 0; i < len; i++){
			if(a[froma+i] < b[fromb+i])
				return -1;
			else if(a[froma+i] > b[fromb+i])
				return 1;
		}
		return 0;
	}
	
	private int findSmallest(char[] a, int from, int to){
		
		int min_pos = from;
		
		for(int i=from+1; i<=to-pivotLen; i++){
			if(strcmp(a, a, min_pos, i, pivotLen)>0)
				min_pos = i;
		}
		
		return min_pos;
	}
	
	private int findPosOfMin(char[] a, char[] b, int from, int to, int[] flag){
		
		int len = a.length;
		int pos1 = findSmallest(a,from,to);
		int pos2 = findSmallest(b,len - to, len - from);
		
		if(strcmp(a,b,pos1,pos2,pivotLen)<0){
			flag[0] = 0;
			return pos1;
		}
		else{
			flag[0] = 1;
			return pos2;
		}	
	}
	
	private int calPosNew(char[] a, int from, int to){
		
		int val=0;
		
		for(int i=from; i<to; i++){
			val = val<<2;
			val += valTable[a[i]-'A'];
		}
		
		return val % numOfBlocks;
	}
	
	public class MyThreadStep1 extends Thread{
		private CountDownLatch threadsSignal;
		
		public MyThreadStep1(CountDownLatch threadsSignal){
			super();
			this.threadsSignal = threadsSignal; 
		}
		
		@Override
		public void run(){
			System.out.println(Thread.currentThread().getName() + "Start..."); 
			Stopwatch stopwatch=Stopwatch.createUnstarted();
								
			String describeline;
			
			int prepos, substart = 0, subend, min_pos = -1;
			
			char[] lineCharArray = new char[readLen];
			
			int[] flag = new int[1];
			
			try{
				synchronized(lock){
					describeline = bfrG.readLine();
					if(describeline != null){
						bfrG.read(lineCharArray, 0, readLen);
						bfrG.read();
					}
				}
				
				
				while(describeline != null){ 
					stopwatch.start();
					prepos = -1;
					if(isReadLegal(lineCharArray)){
						
						substart = 0;
						
						int len = readLen;
						
						char[] revCharArray = new char[len];
						
						for(int i=0; i<len; i++){
							revCharArray[i] = twinTable[lineCharArray[len-1-i]-'A'];
						}
						
						min_pos = findPosOfMin(lineCharArray, revCharArray, 0, k, flag);
						
						int bound = len - k + 1;
						
						for(int i = 1; i < bound; i++){
							
							if(i > (flag[0]==0?min_pos:len-min_pos-pivotLen)){
								
								int temp = (flag[0]==0 ? calPosNew(lineCharArray,min_pos,min_pos+pivotLen):calPosNew(revCharArray,min_pos,min_pos+pivotLen));
								
								min_pos = findPosOfMin(lineCharArray, revCharArray, i, i+k, flag);
								
								if(temp != (flag[0]==0 ? calPosNew(lineCharArray,min_pos,min_pos+pivotLen):calPosNew(revCharArray,min_pos,min_pos+pivotLen))){
									prepos = temp;
									subend = i - 1 + k;			
									substart = i;
								}
								
							}
							
							else{
								
								if(strcmp(lineCharArray, revCharArray, k + i - pivotLen, len - i - k, pivotLen)<0){
									if(strcmp(lineCharArray, flag[0]==0?lineCharArray:revCharArray, k + i - pivotLen, min_pos, pivotLen)<0){
										
										int temp = (flag[0]==0 ? calPosNew(lineCharArray,min_pos,min_pos+pivotLen):calPosNew(revCharArray,min_pos,min_pos+pivotLen));
										
										min_pos = k + i - pivotLen;
										
										if(temp != calPosNew(lineCharArray, min_pos, min_pos+pivotLen)){
											prepos = temp;
											subend = i - 1 + k;
											substart = i;
										}
										
										flag[0]=0;
			
									}
								}
								else{
									if(strcmp(revCharArray, flag[0]==0?lineCharArray:revCharArray, len - i - k, min_pos, pivotLen)<0){
										
										int temp = (flag[0]==0 ? calPosNew(lineCharArray,min_pos,min_pos+pivotLen):calPosNew(revCharArray,min_pos,min_pos+pivotLen));
										
										min_pos = -k - i + len;
										
										if(temp != calPosNew(revCharArray, min_pos, min_pos+pivotLen)){
											prepos = temp;
											subend = i - 1 + k;
											substart = i;
										}
										
										flag[0]=1;
										
										
									}
								}
							}
							
						}
						subend = len;
						prepos = (flag[0]==0 ? calPosNew(lineCharArray,min_pos,min_pos+pivotLen):calPosNew(revCharArray,min_pos,min_pos+pivotLen));
						
					}
					
						stopwatch.stop();
					synchronized(lock){
						describeline = bfrG.readLine();
						if(describeline != null){
							bfrG.read(lineCharArray, 0, readLen);
							bfrG.read();
						}
					}		
				}
				System.out.println("Total algorithm time:"+ stopwatch);
			
			}catch(Exception E){
				System.out.println("Exception caught!");
				E.printStackTrace();
			}
			
			
			threadsSignal.countDown();  
			System.out.println(Thread.currentThread().getName() + "End. Remaining" + threadsSignal.getCount() + " threads");  
			
		}
	}
	
	private void DistributeNodes(int threadNum) throws Exception{
		CountDownLatch threadSignal = new CountDownLatch(threadNum);
		
		frG = new FileReader(inputfile);
		bfrG = new BufferedReader(frG, bufSize);
		fwG = new FileWriter[numOfBlocks];
		bfwG = new BufferedWriter[numOfBlocks];
		
		writeLocks = new Object[numOfBlocks];
		
		File dir = new File("Nodes");
		if(!dir.exists())
			dir.mkdir();
	
		for(int i=0;i<numOfBlocks;i++){
			fwG[i] = new FileWriter("Nodes/nodes"+i);
			bfwG[i] = new BufferedWriter(fwG[i], bufSize);
			writeLocks[i] = new Object();
		}
		
		for(int i=0;i<threadNum;i++){
			Thread t = new MyThreadStep1(threadSignal);
			t.start();
		}
		threadSignal.await();
		System.out.println(Thread.currentThread().getName() + "End."); 
		
		for(int i=0;i<numOfBlocks;i++){
			bfwG[i].close();
			fwG[i].close();
		}
		
		bfrG.close();
		frG.close();
	}
	
	public void Run(int numThreads) throws Exception{
		
		long time1=0;
		
		long t1 = System.currentTimeMillis();
		System.out.println("Distribute Nodes Begin!");	
		DistributeNodes(numThreads);	
		long t2 = System.currentTimeMillis();
		time1 = (t2-t1)/1000;
		System.out.println("Time used for distributing nodes: " + time1 + " seconds!");	
		
	}
	
    public static void main(String[] args){
    	
    	String infile = "E:\\test.txt";
    	int k = 15, numBlocks = 256, pivot_len = 4, numThreads = 1, bufferSize = 8192, readLen = 101;
    	
    	if(args[0].equals("-help")){
    		System.out.print("Usage: java -jar Partition.jar -in InputPath -k k -L readLength[options]\n" +
	        			       "Options Available: \n" + 
	        			       "[-NB numOfBlocks] : (Integer) Number Of Kmer Blocks. Default: 256" + "\n" + 
	        			       "[-p pivotLength] : (Integer) Pivot Length. Default: 6" + "\n" + 
	        			       "[-t numOfThreads] : (Integer) Number Of Threads. Default: 8" + "\n" +
	        			       "[-b bufferSize] : (Integer) Read/Writer Buffer Size. Default: 8192" + "\n");
    		return;
    	}
    	
    	for(int i=0; i<args.length; i+=2){
    		if(args[i].equals("-in"))
    			infile = args[i+1];
    		else if(args[i].equals("-k"))
    			k = new Integer(args[i+1]);
    		else if(args[i].equals("-NB"))
    			numBlocks = new Integer(args[i+1]);
    		else if(args[i].equals("-p"))
    			pivot_len = new Integer(args[i+1]);
    		else if(args[i].equals("-t"))
    			numThreads = new Integer(args[i+1]);
    		else if(args[i].equals("-b"))
    			bufferSize = new Integer(args[i+1]);
    		else if(args[i].equals("-L"))
    			readLen = new Integer(args[i+1]);
    		else{
    			System.out.println("Wrong with arguments. Abort!");
    			return;
    		}
    	}
    	
		
		Partition bdgraph = new Partition(k, infile, numBlocks, pivot_len, bufferSize, readLen);
	
		try{
			
			System.out.println("Program Configuration:");
	    	System.out.print("Input File: " + infile + "\n" +
	    					 "Kmer Length: " + k + "\n" +
	    					 "Read Length: " + readLen + "\n" +
	    					 "# Of Blocks: " + numBlocks + "\n" +
	    					 "Pivot Length: " + pivot_len + "\n" +
	    					 "# Of Threads: " + numThreads + "\n" +
	    					 "R/W Buffer Size: " + bufferSize + "\n");
		
			bdgraph.Run(numThreads);
		
		}
		catch(Exception E){
			System.out.println("Exception caught!");
			E.printStackTrace();
		}
		
	}	
}
	
	

import java.util.*;
import java.math.BigInteger; 
import java.security.MessageDigest; 
import java.security.NoSuchAlgorithmException;
import java.net.*;
import java.io.*;
import java.math.*;

class clientstream
{
	int dc,kc,index;
	long lt;
	String hpw;
	clientstream(int dc,int kc,long lt,String hpw,int index)
	{
		this.dc=dc;
		this.kc=kc;
		this.lt=lt;
		this.hpw=hpw;
		this.index=index;
	}
}


class Server
{
	static HashMap<String,clientstream> h;
	static ECPoint privatekey;
	static ECPoint publickey;
	static EllipticCurve ec;
	static ECPoint gen;
	static EllipticCurve ecc;
	static ArrayList<ECPoint> plist = null;
	static String computeHash(String input)
	{
		try 
		{ 
            MessageDigest md = MessageDigest.getInstance("MD5");  
            byte[] messageDigest = md.digest(input.getBytes()); 
            BigInteger no = new BigInteger(1, messageDigest); 
            String hashtext = no.toString(16); 
            while (hashtext.length() < 32) 
            { 
                hashtext = "0" + hashtext; 
            } 
            return hashtext; 
        }  
  		catch (NoSuchAlgorithmException e) 
  		{ 
            throw new RuntimeException(e); 
  		}
	}
	public static void main(String[] args) throws Exception
	{
		BigInteger p = new BigInteger("730750818665451459101842416358141509840827284927"),a = new BigInteger("648682824076640526685750120045116032276056611995"),b=new BigInteger("13113038265820878912763355168981314473793873122");
		Server s=new Server();
		ecc=new EllipticCurve(a,b,p);
		h=new HashMap<String,clientstream>();
     	SchoofInterface sch=new SchoofInterface();
     	BigInteger order=SchoofInterface.getNP(a,b,p);
     	System.out.println(order);
		System.out.println("Choosen Curve:");
		System.out.println("P="+p+" A="+a+" B="+b);
		Random Ran = new Random();
		TonelliShanks test = new TonelliShanks(p,a,b,500);
		String[] pts = test.getPoints();
		System.out.println("length"+pts.length);
		for(String point : pts) {
                String[] points = point.split(",");
                new ECPoint(ecc,new BigInteger(points[0]),new BigInteger(points[1]));
            }
        plist = ecc.getPointsOnCurve();
        System.out.println(plist.size());
        byte[] barr = p.toByteArray();
        Ran.nextBytes(barr);
        BigInteger tempRand = new BigInteger(barr);
        if(tempRand.compareTo(p)>0){
            tempRand = p.subtract(tempRand.subtract(p));
        }
        BigInteger rand = tempRand;
        System.out.println("Choosen Geneartor:");
        Random r=new Random();

        BigInteger k = BigInteger.valueOf(1);
        int listSize = plist.size();
        ECPoint base = null;

        while(true){
            plist = ecc.getPointsOnCurve();
            listSize = plist.size();
            System.out.println("size  "+listSize);
            int t1 = r.nextInt(listSize);
            try{
               ECPoint temp = plist.get(t1).multiply(k);
            
                if (ecc.onCurve(temp)) {
                    System.out.println("Base point found!");
                    base = temp;
                    break;
                }
                else{
                    System.out.println("checking next!");
                }
            }
            catch(ArithmeticException exception){
                System.out.println("current erroe.. going to next");
            }
        }
        ECPoint gen=base;
        System.out.println(ecc.getp()+" "+ecc.geta()+" "+ecc.getb());
		System.out.println("XG="+gen.getX()+" YG="+gen.getY());
		privatekey = gen.multiply(rand);
		System.out.println("Private key choosen: x="+privatekey.getX()+" y="+privatekey.getY());
		ServerSocket ss=new ServerSocket(1024);
		int clientcount=-1;
		while(true)
		{
			clientstream c;
			Socket s1=ss.accept();
			System.out.println("Client connected");
			DataInputStream in=new DataInputStream(s1.getInputStream());
			DataOutputStream out=new DataOutputStream(s1.getOutputStream());
			String hpw;
			Integer cid;
			while(true)
			{
				out.writeUTF(gen.getX()+","+gen.getY());
				cid=Integer.parseInt(in.readUTF());
				hpw=in.readUTF();
				System.out.println(cid+" "+hpw);
				String temp=computeHash(""+cid+privatekey.getX()+privatekey.getY());
				if(h.containsKey(temp))
				{
					out.writeUTF("no");
				}
				else
				{
					c=new clientstream(Ran.nextInt(100000),Ran.nextInt(100000),new Date().getTime()+10000,hpw,++clientcount);
					h.put(temp,c);
					out.writeUTF("yes");
					break;
				}
			}
			String kci=computeHash(""+cid+c.dc+c.kc+c.index);
			System.out.println("kci:"+kci);
			String pkci=(new BigInteger(computeHash(""+c.hpw+cid),16).xor(new BigInteger(kci,16))).toString(16);
			System.out.println("pkci "+pkci);
			out.writeUTF(pkci);
			String pidci=cid+","+c.dc+","+c.lt+","+c.index;
			BigInteger d = BigInteger.valueOf(13);
			System.out.println("pidci:"+pidci);
			ArrayList<ECPoint> enc = Elgamal.Encryption(p, a, b, d, base,order,pidci,plist);
			pidci=enc.toString();
			out.writeUTF(enc.toString());
			/*ArrayList<ECPoint> al=Elgamal.fromStringToArrayList(enc.toString(),ecc);
			System.out.println(al);*/
			System.out.println("Authentication phase starts:\n");
			String tpidci=in.readUTF();
			String tRcj=in.readUTF();
			System.out.println();
			System.out.println(tRcj);
			String tauthcj=in.readUTF();
			ArrayList<ECPoint> ps=Elgamal.fromStringToArrayList(tpidci,ecc);
			String t[]=Elgamal.dec(ecc,ps,d,p,order).split(",");
			String k_ci=computeHash(""+cid+t[1]+c.kc+c.index);
			System.out.println("k'ci"+k_ci);
			String cAuth=computeHash(cid+""+tpidci+""+tRcj+""+k_ci);
			System.out.println(cAuth);
			System.out.println(tauthcj);
			if(cAuth.equals(tauthcj))
				System.out.println("--------------M1--------- Successful");
			String temporary[]=tRcj.split(",");
			ECPoint Rcj=new ECPoint(ecc,new BigInteger(temporary[0].replaceAll("[^0-9]","")),new BigInteger(temporary[1].replaceAll("[^0-9]","")));
			BigInteger rsi=new BigInteger(""+r.nextInt(100000000));
			ECPoint Rij=Rcj.multiply(rsi);
			System.out.println("Rij"+Rij);
			ECPoint Rsi=gen.multiply(rsi);
			String authsi=computeHash(""+Rij.toString()+Rcj.toString()+Rsi.toString()+cid+kci);
			out.writeUTF(authsi);
			out.writeUTF(Rsi.toString());
			System.out.println("Session key computation");
			String conf1=in.readUTF();
			String ski=computeHash(cid+Rij.toString()+k_ci);
			String conf=computeHash(Rij.toString()+ski+Rsi.toString()+k_ci);
			if(conf.equals(conf1))
				System.out.println("--------------M3--------- Successful");
		}
	}
}
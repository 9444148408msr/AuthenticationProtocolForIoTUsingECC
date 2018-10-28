import java.util.*;
import java.math.BigInteger; 
import java.security.MessageDigest; 
import java.security.NoSuchAlgorithmException;
import java.net.*;
import java.io.*;

class SmartCard
{
	String pidci;
	String upkci;
	SmartCard(String pidci,String upkci)
	{
		this.pidci=pidci;
		this.upkci=upkci;	
	}
}

class Client
{
	int id;
	int pwd;
	int xc;
	DataOutputStream out;
	DataInputStream in;
	SmartCard sc;
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
		Client c=new Client();
		Random r=new Random();
		Socket s=new Socket("localhost",1024);
		c.out=new DataOutputStream(s.getOutputStream());
		c.in=new DataInputStream(s.getInputStream());
		BigInteger p = new BigInteger("730750818665451459101842416358141509840827284927"),a = new BigInteger("648682824076640526685750120045116032276056611995"),b=new BigInteger("13113038265820878912763355168981314473793873122");
		EllipticCurve ecc=new EllipticCurve(a,b,p);
		String[] z=c.in.readUTF().split(",");
		ECPoint gen=new ECPoint(ecc,new BigInteger(z[0].trim()),new BigInteger(z[1].trim()));
		String hpwc;
		do
		{
			c.id=r.nextInt(1000000000);
			c.pwd=r.nextInt(1000000000);
			c.xc=r.nextInt(1000000000);
			System.out.println("Id choosen: "+c.id);
			System.out.println("Password choosen: "+c.pwd);
			System.out.println("xc: "+c.xc);
			hpwc=computeHash(c.pwd+""+c.xc);
			System.out.println("After finding hash of the given password is: "+hpwc);
			c.out.writeUTF(""+c.id);
			c.out.writeUTF(hpwc);
		}while(c.in.readUTF().indexOf("no")!=-1);
		String pkci=c.in.readUTF();
		String temp1=computeHash(hpwc+c.id);
		String temp2=computeHash(""+c.pwd+c.id);
		String upkcij=new BigInteger(pkci,16).xor(new BigInteger(temp1,16)).xor(new BigInteger(temp2,16)).toString(16);
		System.out.println("upkcij "+upkcij);
		String pidci=c.in.readUTF();
		System.out.println(pidci);
		c.sc=new SmartCard(pidci,upkcij);
		System.out.println("Authentication phase starts:\n");
		int rcJ=r.nextInt(1000000);
		BigInteger rcj=new BigInteger(""+rcJ);
		ECPoint Rcj=gen.multiply(rcj);
		String intermediate=computeHash(c.pwd+""+c.id);
		String kci=new BigInteger(c.sc.upkci,16).xor(new BigInteger(intermediate,16)).toString(16);
		String authcj=computeHash(c.id+""+c.sc.pidci+""+Rcj.toString()+kci);
		System.out.println("Sending authentication request (m1)\n");
		c.out.writeUTF(pidci);
		c.out.writeUTF(Rcj.toString());
		c.out.writeUTF(authcj);
		String sAuthsi=c.in.readUTF();
		String sRsi=c.in.readUTF();
		String temporary[]=sRsi.split(",");
		ECPoint Rsi=new ECPoint(ecc,new BigInteger(temporary[0].replaceAll("[^0-9]","")),new BigInteger(temporary[1].replaceAll("[^0-9]","")));
		ECPoint Rji=Rsi.multiply(rcj);
		System.out.println("Rji"+Rji);
		String computedAuthsi=computeHash(Rji.toString()+Rcj.toString()+sRsi+c.id+kci);
		System.out.println(computedAuthsi);
		System.out.println(sAuthsi);
		if(sAuthsi.equals(computedAuthsi))
			System.out.println("--------------M2--------- Successful");
		String skj=computeHash(c.id+""+Rji.toString()+kci);
		String conf=computeHash(Rji.toString()+skj+Rsi.toString()+kci);
		c.out.writeUTF(conf);
	}
}
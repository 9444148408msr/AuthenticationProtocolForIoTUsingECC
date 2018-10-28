import java.math.*;
import java.util.*;
import java.util.function.*;
import java.io.*;
import java.security.*;
import javax.crypto.*;
import java.security.*;
import java.security.spec.*;

interface ECParameters 
{
    public BigInteger a();
    public BigInteger b();
    public BigInteger p();
    public BigInteger generatorX();   
    public BigInteger generatorY(); 
    public BigInteger order();
    public String toString();
}

class NoCommonMotherException extends Exception
{
    public String getErrorString()
    {
        return "NoCommonMother";
    }
}

class NotOnMotherException extends Exception
{
    private ECPoint sender;
    public NotOnMotherException(ECPoint sender)
    {
        this.sender = sender;
    }
    public String getErrorString()
    {
        return "NotOnMother";
    }
    public ECPoint getSource()
    {
        return sender;
    }
}

class InsecureCurveException extends Exception
{
    public static final int NONPRIMEMODULUS = -1;
    public static final int SINGULAR = 0;
    public static final int SUPERSINGULAR = 1;
    public static final int ANOMALOUS = 2;
    public static final int TRACEONE = 3;
    private int error;
    private EllipticCurve sender;
    public InsecureCurveException(EllipticCurve sender)
    {
        error = SINGULAR;
        this.sender = sender;
    }
    public InsecureCurveException(int e, EllipticCurve sender)
    {
        error = e;
        this.sender = sender;
    }
    public int getError(){
    return error;
    }

    public String getErrorString(){
    if (error == SINGULAR) return "SINGULAR";
    else if (error == NONPRIMEMODULUS) return "NONPRIMEMODULUS"; 
    else if (error == SUPERSINGULAR) return "SUPERSINGULAR"; 
    else if (error == ANOMALOUS) return "ANOMALOUS"; 
    else if (error == TRACEONE) return "TRACEONE"; 
    else return "UNKNOWN ERROR"; 
    }

    public EllipticCurve getSender(){
    return sender;
    }
}



class SchoofInterface {
    public static String Directory = System.getProperty("user.dir");
    public static String FileName = "/schoof.exe";
    
    public static BigInteger getNP(BigInteger A, BigInteger B, BigInteger P)
    {
        BigInteger NP = null;
        
        try
        {
            Process Proc = Runtime.getRuntime().exec(Directory + FileName + " " + P + " " + A + " " + B);
            BufferedReader BR = new BufferedReader(new InputStreamReader(Proc.getInputStream()));

            String Line = null;
            while ((Line = BR.readLine()) != null)
            {
                if(Line.contains("composite")) return null;
                else
                {
                if (!Line.startsWith("NP= "))
                    continue;

                int Stop = 0;
                for (Stop = 4; Stop < Line.length() && Character.isDigit(Line.charAt(Stop)); Stop++);

                NP = new BigInteger(Line.substring(4, Stop));
                break;
                }
            }
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
        
        return NP;
    }
}




class EllipticCurve {

    private BigInteger a, b, p, order;
    private ECPoint generator;
    private BigInteger ppodbf;
    private int pointcmpsize;
    private String name;
    
    List<String> temparr = new ArrayList<String>(1000);
    private static BigInteger primeNumber = new BigInteger(Main.BIG_INT_VALUE);
    private static BigInteger maxNum;
    public static final BigInteger COEFA = new BigInteger("4");
    public static final BigInteger COEFB = new BigInteger("27");
    public static final int PRIMESECURITY = 500;
    static
    {
        maxNum = bigIntSqRootFloor(primeNumber).multiply(BigInteger.valueOf(2)).add(primeNumber).add(BigInteger.valueOf(1));
    }
    public EllipticCurve(BigInteger a, BigInteger b, BigInteger p) throws InsecureCurveException {

    this.a = a;
    this.b = b;
    this.p = p;
    if (!p.isProbablePrime(PRIMESECURITY)) {
            }
    if (isSingular()) throw new InsecureCurveException(InsecureCurveException.SINGULAR, this);
    
    byte[] pb = p.toByteArray();
    if(pb[0] == 0) pointcmpsize = pb.length;
    else pointcmpsize = pb.length + 1;
    name = "";    
    }

    public EllipticCurve(ECParameters ecp) throws InsecureCurveException {
    this(ecp.a(),ecp.b(),ecp.p());
    order = ecp.order();
    name = ecp.toString();
    try{
        generator = new ECPoint(this, ecp.generatorX(), ecp.generatorY());
        generator.fastCache();
    }
    catch (NotOnMotherException e){
        System.out.println("Error defining EllipticCurve: generator not on mother!");
    }
    }

    public void writeCurve(DataOutputStream output) throws IOException {
    byte[] ab = a.toByteArray();
    output.writeInt(ab.length);
    output.write(ab);
    byte[] bb = b.toByteArray();
    output.writeInt(bb.length);
    output.write(bb);
    byte[] pb = p.toByteArray();
    output.writeInt(pb.length);
    output.write(pb);
    byte[] ob = order.toByteArray();
    output.writeInt(ob.length);
    output.write(ob);
    byte[] gb = generator.compress();
    output.writeInt(gb.length);
    output.write(gb);
    byte[] ppb = getPPODBF().toByteArray();
    output.writeInt(ppb.length);
    output.write(ppb);
    output.writeInt(pointcmpsize);
    output.writeUTF(name);
    }

    protected EllipticCurve(DataInputStream input) throws IOException {
    byte[] ab = new byte[input.readInt()];
    input.read(ab);
    a = new BigInteger(ab);
    byte[] bb = new byte[input.readInt()];
    input.read(bb);
    b = new BigInteger(bb);
    byte[] pb = new byte[input.readInt()];
    input.read(pb);
    p = new BigInteger(pb);
    byte[] ob = new byte[input.readInt()];
    input.read(ob);
    order = new BigInteger(ob);
    byte[] gb = new byte[input.readInt()];
    input.read(gb);
    generator = new ECPoint(gb, this);
    byte[] ppb = new byte[input.readInt()];
    input.read(ppb);
    ppodbf = new BigInteger(ppb);
    pointcmpsize = input.readInt();
    name = input.readUTF();
    generator.fastCache();
    }
    
    public static BigInteger bigIntSqRootFloor(BigInteger x)
            throws IllegalArgumentException {
        if (x.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("Negative argument.");
        }

        if (x .equals(BigInteger.ZERO) || x.equals(BigInteger.ONE)) {
            return x;
        } 
        BigInteger two = BigInteger.valueOf(2L);
        BigInteger y;

        for (y = x.divide(two);
                y.compareTo(x.divide(y)) > 0;
                y = ((x.divide(y)).add(y)).divide(two));
        return y;
    }

    public boolean isSingular(){

    BigInteger aa = a.pow(3);
    BigInteger bb = b.pow(2);

    BigInteger result = ((aa.multiply(COEFA)).add(bb.multiply(COEFB) ) ).mod(p);

    if ( result.compareTo(BigInteger.ZERO) == 0 ) return true;
    else return false;

    }

    
    public BigInteger calculateOrder(){
    return null;
    }

    
    public ECPoint calculateGenerator(){
    return null;
    }
    ArrayList<ECPoint> pointsOnCurve=new ArrayList<ECPoint>();
    public ArrayList<ECPoint> getPointsOnCurve() {
        return pointsOnCurve;
    }


    public void setPointsOnCurve(ArrayList<ECPoint> pointsOnCurve) {
        this.pointsOnCurve = pointsOnCurve;
    }
    public boolean onCurve(ECPoint q){
    ECPoint t=q;
    if (q.isZero()){
        if(!pointsOnCurve.contains(q))
            {
                pointsOnCurve.add(q);
            }
        return true;
    }
    BigInteger y_square = (q.getY()).modPow(new BigInteger("2"),p);
    BigInteger x_cube = (q.getX()).modPow(new BigInteger("3"),p);
    BigInteger x = q.getX();

    BigInteger dum = ((x_cube.add(a.multiply(x))).add(b)).mod(p);
          
    if (y_square.compareTo(dum) == 0) {

        if(temparr.contains(q.getX().mod(primeNumber).toString()+","+q.getY().mod(primeNumber).toString()) == false){
            temparr.add(q.getX().mod(primeNumber).toString()+","+q.getY().mod(primeNumber).toString());
            pointsOnCurve.add(q);
        }

        
        
        return true;
    }
    else
        {
        
        return false;
        }

    }

   
    public BigInteger getOrder(){
    return order;
    }

    public ECPoint getZero(){
    return new ECPoint(this);
    }

    public BigInteger geta(){
    return a;
    }

    public BigInteger getb(){
    return b;
    }

    public BigInteger getp(){
    return p;
    }

    public int getPCS() {
    return pointcmpsize;
    }

    public ECPoint getGenerator(){
    return generator;
    }

    public String toString(){
    if (name == null) return "y^2 = x^3 + "  + a + "x + " + b + " ( mod " + p + " )";
    else if (name.equals("")) return "y^2 = x^3 + "  + a + "x + " + b + " ( mod " + p + " )";
    else return name;
    }

    public BigInteger getPPODBF(){
    if(ppodbf == null) {
        ppodbf = p.add(BigInteger.ONE).shiftRight(2);
    }
    return ppodbf;
    }
}



class TonelliShanks {
    private static final BigInteger ZERO = BigInteger.ZERO;
    private static final BigInteger ONE = BigInteger.ONE;
    private static final BigInteger TEN = BigInteger.TEN;
    private static final BigInteger TWO = BigInteger.valueOf(2);
    private static final BigInteger FOUR = BigInteger.valueOf(4);
    BigInteger p1;
    BigInteger a;
    BigInteger b;
    int temp;
    TonelliShanks(BigInteger prime,BigInteger a,BigInteger b,int t)
    {
        p1=prime;
        this.a=a;
        this.b=b;
        temp=t;
    }
 
    private static class Solution {
        private BigInteger root1;
        private BigInteger root2;
        private boolean exists;
 
        Solution(BigInteger root1, BigInteger root2, boolean exists) {
            this.root1 = root1;
            this.root2 = root2;
            this.exists = exists;
        }
    }
 
    private static Solution ts(Long n, Long p) {
        return ts(BigInteger.valueOf(n), BigInteger.valueOf(p));
    }
 
    private static Solution ts(BigInteger n, BigInteger p) {
        BiFunction<BigInteger, BigInteger, BigInteger> powModP = (BigInteger a, BigInteger e) -> a.modPow(e, p);
        Function<BigInteger, BigInteger> ls = (BigInteger a) -> powModP.apply(a, p.subtract(ONE).divide(TWO));
 
        if (!ls.apply(n).equals(ONE)) return new Solution(ZERO, ZERO, false);
 
        BigInteger q = p.subtract(ONE);
        BigInteger ss = ZERO;
        while (q.and(ONE).equals(ZERO)) {
            ss = ss.add(ONE);
            q = q.shiftRight(1);
        }
 
        if (ss.equals(ONE)) {
            BigInteger r1 = powModP.apply(n, p.add(ONE).divide(FOUR));
            return new Solution(r1, p.subtract(r1), true);
        }
 
        BigInteger z = TWO;
        while (!ls.apply(z).equals(p.subtract(ONE))) z = z.add(ONE);
        BigInteger c = powModP.apply(z, q);
        BigInteger r = powModP.apply(n, q.add(ONE).divide(TWO));
        BigInteger t = powModP.apply(n, q);
        BigInteger m = ss;
 
        while (true) {
            if (t.equals(ONE)) return new Solution(r, p.subtract(r), true);
            BigInteger i = ZERO;
            BigInteger zz = t;
            while (!zz.equals(BigInteger.ONE) && i.compareTo(m.subtract(ONE)) < 0) {
                zz = zz.multiply(zz).mod(p);
                i = i.add(ONE);
            }
            BigInteger b = c;
            BigInteger e = m.subtract(i).subtract(ONE);
            while (e.compareTo(ZERO) > 0) {
                b = b.multiply(b).mod(p);
                e = e.subtract(ONE);
            }
            r = r.multiply(b).mod(p);
            c = b.multiply(b).mod(p);
            t = t.multiply(c).mod(p);
            m = i;
        }
    }
 
    String[] getPoints(){
        int j=0;
        String a1[]=new String[temp];
        for(BigInteger i=p1.subtract(BigInteger.ONE);i.compareTo(BigInteger.ZERO)>=0;i=i.subtract(BigInteger.ONE))
        {
        BigInteger rhs=i.multiply(i).multiply(i).add(a.multiply(i)).add(b).mod(p1);
        Solution sol = ts(rhs, p1);
        if (sol.exists) {
            //System.out.printf("root2 = %s\n", sol.root2);
            a1[j]=i.toString()+","+sol.root1.toString();
            j++;
            if(j%temp==0)
                break;
        }  
        }
        return a1;
    }
}

class ECPoint {

    public final static BigInteger TWO = new BigInteger("2");
    public final static BigInteger THREE = new BigInteger("3");

    private EllipticCurve mother;

    private BigInteger x, y;
    private boolean iszero;

    private ECPoint[] fastcache = null;
    private ECPoint[] cache = null;

    public void fastCache() {
    try {
        if(fastcache == null) {
        fastcache = new ECPoint[256];
        fastcache[0] = new ECPoint(mother);
        for(int i = 1; i < fastcache.length; i++) {
            fastcache[i] = fastcache[i-1].add(this);
        }
        }
    } catch (NoCommonMotherException e) {
        System.out.println("ECPoint.fastcache: THIS CANNOT HAPPEN!!!");
    }
    }
    
    BigInteger p=new BigInteger(Main.BIG_INT_VALUE);
    public ECPoint(EllipticCurve mother, BigInteger x, BigInteger y) throws NotOnMotherException{
    this.mother = mother;
    this.x = x;
    this.y = y;
    
    if (mother.onCurve(this))
        {
            //System.out.println(x.mod(p)+" "+y.mod(p));
        }
    iszero = false;
    }

    
    public BigInteger getX() {
        return x;
    }


    public void setX(BigInteger x) {
        this.x = x;
    }


    public BigInteger getY() {
        return y;
    }


    public void setY(BigInteger y) {
        this.y = y;
    }


    


    public ECPoint(byte[] bytes, EllipticCurve mother) {
    this.mother = mother;
    if (bytes[0] == 2){
        iszero = true;
        return;
    }
    boolean ymt = false;
    if(bytes[0] != 0) ymt = true;
    bytes[0] = 0;
    x = new BigInteger(bytes);
    if(mother.getPPODBF() == null) System.out.println("Error!!!");
    y = x.multiply(x).add(mother.geta()).multiply(x).add(mother.getb()).modPow(mother.getPPODBF(),mother.getp());
    if(ymt != y.testBit(0)) {
        y = mother.getp().subtract(y);
    }
    iszero = false;
    }
   
    public ECPoint(EllipticCurve e){
    x = y = BigInteger.ZERO;
    mother = e;
    iszero = true;
    }

    public byte[] compress() {
    byte[] cmp = new byte[mother.getPCS()];
    if (iszero){
        cmp[0] = 2;
    }
    byte[] xb = x.toByteArray();
    System.arraycopy(xb, 0, cmp, mother.getPCS()-xb.length, xb.length);
    if(y.testBit(0)) cmp[0] = 1;
    return cmp;
    }

   
    public ECPoint add(ECPoint q) throws NoCommonMotherException{

    if (!hasCommonMother(q)) throw new NoCommonMotherException();

    if (this.iszero) return q;
    else if (q.isZero()) return this;

    BigInteger y1 = y;
    BigInteger y2 = q.getY();
    BigInteger x1 = x;
    BigInteger x2 = q.getX();

    BigInteger alpha;

    if (x2.compareTo(x1) == 0) {

        if (!(y2.compareTo(y1) == 0)) return new ECPoint(mother);
        else {
        alpha = ((x1.modPow(TWO,mother.getp())).multiply(THREE)).add(mother.geta());
        alpha = (alpha.multiply((TWO.multiply(y1)).modInverse(mother.getp()))).mod(mother.getp());
        }

    } else {
        alpha = ((y2.subtract(y1)).multiply((x2.subtract(x1)).modInverse(mother.getp()))).mod(mother.getp());
    } 

    BigInteger x3,y3;
    x3 = (((alpha.modPow(TWO,mother.getp())).subtract(x2)).subtract(x1)).mod(mother.getp());
    y3 = ((alpha.multiply(x1.subtract(x3))).subtract(y1)).mod(mother.getp());
 

    try{ return new ECPoint(mother,x3,y3); }
    catch (NotOnMotherException e){
        System.out.println("Error in add");
        return null;
    }

    }
    
    public ECPoint multiply(BigInteger coef) {
    try{
        ECPoint result = new ECPoint(mother);
        byte[] coefb = coef.toByteArray();
        if(fastcache != null) {
        for(int i = 0; i < coefb.length; i++) {
            result = result.times256().add(fastcache[coefb[i]&255]);
        }
        return result;
        }
        if(cache == null) {
        cache = new ECPoint[16];
        cache[0] = new ECPoint(mother);
        for(int i = 1; i < cache.length; i++) {
            cache[i] = cache[i-1].add(this);
        }
        }
        for(int i = 0; i < coefb.length; i++) {
        result = result.times16().add(cache[(coefb[i]>>4)&15]).times16().add(cache[coefb[i]&15]);
        }
        return result;
    } catch (NoCommonMotherException e) {
        System.out.println("Error in pow");
        return null;
    }
    }

    private ECPoint times16() {
    try {
        ECPoint result = this;
        for(int i = 0; i < 4; i++) {
        result = result.add(result);
        }
        return result;
    } catch (Exception e) {
        System.out.println("ECPoint.times16: THIS CANNOT HAPPEN!!!");
        return null;
    }
    }

    private ECPoint times256() {
    try {
        ECPoint result = this;
        for(int i = 0; i < 8; i++) {
        result = result.add(result);
        }
        return result;
    } catch (Exception e) {
        System.out.println("ECPoint.times256: THIS CANNOT HAPPEN!!!");
        return null;
    }
    }
   public EllipticCurve getMother(){
    return mother;
    }

    public String toString(){
    return "(" + x.toString() + ", " + y.toString() + ")";
    }

    public boolean hasCommonMother(ECPoint p){
    if (this.mother.equals(p.getMother())) return true;
    else return false;
    }

    public boolean isZero(){
    return iszero;
    }
}



class Elgamal {
    static ArrayList<ECPoint> plist = null;
    static ECPoint C1,Public;
    static BigInteger R;
    static BigInteger S;
    static BigInteger digesthash;
    static ECPoint BASE;
    static int len=0;
    static int no_of_blocks = 0;
    private static boolean stop = false;
    public static ECPoint addInverse(EllipticCurve e1, ECPoint point,BigInteger prime) throws NotOnMotherException
    {
        BigInteger y0 = point.getY().negate();
        while(y0.compareTo(BigInteger.ZERO)<=0){
            y0 = y0.add(prime);
        }
        BigInteger y = y0;
        BigInteger x=point.getX();
        ECPoint pt=new ECPoint(e1,x,y);
        return pt;
    }
    
    public static ArrayList<ECPoint> Encryption(BigInteger P, BigInteger a, BigInteger b, BigInteger sk, ECPoint base,BigInteger order)
            throws NoCommonMotherException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException {
        File fin=new File(System.getProperty("user.dir") + "/Siva.txt");
        Scanner sc=new Scanner(fin);
        String msg="";
        Random Ran = new Random();
        BASE=base;
        while(sc.hasNext())
        {
            msg+=sc.nextLine();
            
        }
         len=msg.length();
        System.out.println("\nPlaintext\n"+ msg);
        System.out.println("Signature starts"); 
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(msg.getBytes()); 
        byte byteData[] = md.digest(); 
        BigInteger hash = new BigInteger(1,byteData);
       BigInteger z=hash.shiftRight(hash.bitLength()-96);
       KeyPairGenerator kg=KeyPairGenerator.getInstance("EC");
        SecureRandom sr=SecureRandom.getInstance("SHA1PRNG");
        kg.initialize(256,sr);
        KeyPair kp=kg.generateKeyPair();
        PrivateKey priv=kp.getPrivate();
        PublicKey publ=kp.getPublic();
        Signature sig=Signature.getInstance("SHA1withECDSA");
        sig.initSign(priv);
        String filename=System.getProperty("user.dir") + "/Siva.txt";
        FileInputStream fis=new FileInputStream(filename);
        BufferedInputStream bis=new BufferedInputStream(fis);
        byte[] by=new byte[1024];
        int size;
            while(bis.available()!=0)
            {
                size=bis.read(by);
                sig.update(by,0,size);
            }
        bis.close();
        byte[] rsign=sig.sign();
        FileOutputStream fos=new FileOutputStream(System.getProperty("user.dir") + "/rsign.txt");

        fos.write(rsign);
        fos.close();
       
       byte[] key=publ.getEncoded();
        FileOutputStream fs=new FileOutputStream(System.getProperty("user.dir") + "/pk.txt");
        fs.write(key);
        fs.close();
            
        byte[] barr = P.toByteArray();
        Ran.nextBytes(barr);
        BigInteger tempRand = new BigInteger(barr);
        if(tempRand.compareTo(P)>0){
            tempRand = P.subtract(tempRand.subtract(P));
        }
        
       
        
        ArrayList<ECPoint> C2 = new ArrayList<ECPoint>();
        BigInteger rand = tempRand;
        
        barr = P.toByteArray();
        Ran.nextBytes(barr);
        tempRand = new BigInteger(barr);
        if(tempRand.compareTo(P)>0){
            tempRand = P.subtract(tempRand.subtract(P));
        }   
        BigInteger k1 = tempRand;
        ECPoint R = base.multiply(rand);
        System.out.println("Private key value : "+R.getX()+" "+R.getY());
        ECPoint Q = R.multiply(sk);
        ECPoint pub=base.multiply(sk);
        System.out.println("Public key value : "+pub.getX()+" "+pub.getY());
        Public =pub;
         C1 = R.multiply(k1);
        ECPoint Ctemp = Q.multiply(k1);
        for (int i = 0; i < msg.length(); i++) {
         
            char[][] block = new char[8][8];
            no_of_blocks++;
            for(int iter1 =0; iter1<8;iter1++) {
                for(int iter2 = 0;iter2<8;iter2++){
                    if(i<msg.length()){
                        if(iter1%2==0){
                            block[iter1][iter2] = msg.charAt(i);
                            ECPoint m = getPoint((msg.charAt(i)-' '));
                                ECPoint tem = m.add(Ctemp);
                                C2.add(tem);
                        }
                        else{
                            block[iter1][7-iter2] = msg.charAt(i);
                            ECPoint m = getPoint((msg.charAt(i) - ' '));
                                ECPoint tem = m.add(Ctemp);
                                C2.add(tem);
                        }
                        i++;
                    }
                    else {
                        if(iter1%2==0){
                            block[iter1][iter2] = ' ';
                        }
                        else{
                            block[iter1][7-iter2] = ' ';
                        }
                        i++;
                    }
                }
            }
            i--;
        }
        System.out.println("Encrypted " + no_of_blocks + " blocks of text");
        return C2;

    }



    public static ArrayList<ECPoint> fromStringToArrayList(String s,EllipticCurve ecc) throws Exception
    {
        ArrayList<ECPoint> po=new ArrayList<ECPoint>();
        String[] st=s.split(",");
        for(int i=0;i<st.length;i++)
        {
            st[i]= st[i].replaceAll("[^0-9]", "");
        }
        for(int i=0;i<st.length;i+=2)
        {
            po.add(new ECPoint(ecc,new BigInteger(st[i]),new BigInteger(st[i+1])));
        }
        return po;
    }


     public static ArrayList<ECPoint> Encryption(BigInteger P, BigInteger a, BigInteger b, BigInteger sk, ECPoint base,BigInteger order, String pt,ArrayList<ECPoint> pl)
            throws NoCommonMotherException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException {
        plist=pl;
        String msg=pt;
        Random Ran = new Random();
        BASE=base;
        len=msg.length();
        System.out.println("\nPlaintext\n"+ msg);
        byte[] barr = P.toByteArray();
        Ran.nextBytes(barr);
        BigInteger tempRand = new BigInteger(barr);
        if(tempRand.compareTo(P)>0){
            tempRand = P.subtract(tempRand.subtract(P));
        }
        
        ArrayList<ECPoint> C2 = new ArrayList<ECPoint>();
        BigInteger rand = tempRand;
        
        barr = P.toByteArray();
        Ran.nextBytes(barr);
        tempRand = new BigInteger(barr);
        if(tempRand.compareTo(P)>0){
            tempRand = P.subtract(tempRand.subtract(P));
        }   
        BigInteger k1 = tempRand;
        ECPoint R = base.multiply(rand);
        System.out.println("Private key value : "+R.getX()+" "+R.getY());
        ECPoint Q = R.multiply(sk);
        ECPoint pub=base.multiply(sk);
        System.out.println("Public key value : "+pub.getX()+" "+pub.getY());
        Public =pub;
        C1 = R.multiply(k1);
        ECPoint Ctemp = Q.multiply(k1);
        for (int i = 0; i < msg.length(); i++) {
         
            char[][] block = new char[8][8];
            no_of_blocks++;
            for(int iter1 =0; iter1<8;iter1++) {
                for(int iter2 = 0;iter2<8;iter2++){
                    if(i<msg.length()){
                        if(iter1%2==0){
                            block[iter1][iter2] = msg.charAt(i);
                            ECPoint m = getPoint((msg.charAt(i)-' '));
                                ECPoint tem = m.add(Ctemp);
                                C2.add(tem);
                        }
                        else{
                            block[iter1][7-iter2] = msg.charAt(i);
                            ECPoint m = getPoint((msg.charAt(i) - ' '));
                                ECPoint tem = m.add(Ctemp);
                                C2.add(tem);
                        }
                        i++;
                    }
                    else {
                        if(iter1%2==0){
                            block[iter1][iter2] = ' ';
                        }
                        else{
                            block[iter1][7-iter2] = ' ';
                        }
                        i++;
                    }
                }
            }
            i--;
        }
        //System.out.println("Encrypted " + no_of_blocks + " blocks of text");
        return C2;
    }

    public static ArrayList<ECPoint> Decrypt(EllipticCurve e1,ArrayList<ECPoint> C2,BigInteger d,BigInteger prime,BigInteger order)throws NotOnMotherException, NoCommonMotherException, SignatureException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException
    {
        boolean verify=true;
        if(verify)
        {
            System.out.println("Verified Successfully ");
            ArrayList<ECPoint> tempp=new ArrayList<ECPoint>();
        
            ECPoint mul=C1.multiply(d);
            ECPoint negmul=addInverse(e1, mul, prime);
        
            for(int i=0;i<len;i++)
            {
            ECPoint t1=C2.get(i);
            ECPoint temp=negmul.add(t1);
            tempp.add(temp);
            
            }
            System.out.println("Decrypted "+ no_of_blocks + " blocks of cipher text");
        return tempp;
        }
        else
        {
            System.out.println("Verification Failed and IF you want access try to break..........");
            return null;
        }
    }

    public static String dec(EllipticCurve e,ArrayList<ECPoint> enc,BigInteger d,BigInteger prime,BigInteger order) throws NotOnMotherException, NoCommonMotherException, SignatureException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException
    {
        ArrayList<ECPoint> dec=Decrypt(e,enc, d, prime,order);
        int cnt=0,index=-1;
            boolean f=false;
            String text="";
            for(ECPoint i:dec)
            {
                index=-1;
                cnt=0;
                for(ECPoint j:plist)
                {
                    if(i.getX().equals(j.getX()) && i.getY().equals(j.getY()))
                     {
                        index=cnt;
                        
                        f=true;
                        break;
                     }
                    else
                        cnt++;
                }
                if(f)
                {
                    text+=(char)(index+' ');
                }
            }
        return text.trim();
    }

    public static ECPoint getPoint(int ascii) {
        return plist.get(ascii);
    }
    public  void Elgamall(BigInteger P,BigInteger A,BigInteger B,BigInteger order) {
        try {

            BigInteger a = A;
            BigInteger b = B;
            BigInteger prime = P;
            EllipticCurve e = new EllipticCurve(a, b, prime);
            System.out.println("EllipticCurve: " + e + " created succesfully!");
            ECPoint base = null;
            Random rand = new Random();
            TonelliShanks test = new TonelliShanks(prime,a,b,5);
            String[] pts = test.getPoints();
            for(String point : pts) {
                String[] points = point.split(",");
                new ECPoint(e,new BigInteger(points[0]),new BigInteger(points[1]));
            }
            plist = e.getPointsOnCurve();
            
            BigInteger k = BigInteger.valueOf(1);
            int in=0;
            int listSize = plist.size();
            while(true){
                plist = e.getPointsOnCurve();
                listSize = plist.size();
                System.out.println("size  "+listSize);
                Random r = new Random();
                int t1 = r.nextInt(listSize);
                try{
                    ECPoint temp = plist.get(t1).multiply(k);
                
                    if (e.onCurve(temp)) {
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
            System.out.println("\nBASE POINT");
            System.out.println("XG: " + base.getX() + " YG: " + base.getY());
            Random r=new Random();
            BigInteger d = BigInteger.valueOf(13);
            
            ArrayList<ECPoint> enc = Encryption(prime, a, b, d, base,order);
            System.out.println("\nSigncrypting.........");
            String emsg="";
            File fenc = new File(System.getProperty("user.dir") + "/enc.txt");
            FileOutputStream fos = new FileOutputStream(fenc);
         
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            for (ECPoint i : enc) {
                bw.write(i.getX()+""+i.getY());
            }
            
            bw.close();
            System.out.println("Signed and Encrypted Successfully..........");
            System.out.println("\nUnsigncrypting.........");
            ArrayList<ECPoint> dec=Decrypt(e,enc, d, prime,order);
            int cnt=0,index=-1;
            boolean f=false;
            String text="";
            for(ECPoint i:dec)
            {
                index=-1;
                cnt=0;
                for(ECPoint j:plist)
                {
                    if(i.getX().equals(j.getX()) && i.getY().equals(j.getY()))
                     {
                        index=cnt;
                        f=true;
                        break;
                     }
                    else
                        cnt++;
                }
                if(f)
                {
                    text+=(char)(index+' ');
                }
            }
            String res="";
            File fdec = new File(System.getProperty("user.dir") + "/dec.txt");
            FileOutputStream fou=new FileOutputStream(fdec);
         
            BufferedWriter bw1 = new BufferedWriter(new OutputStreamWriter(fou));
            for(int i=0;i<text.length();i++)
            {
                if(text.charAt(i)==' ')
                    {
                    bw1.newLine();
                    res+=" \n";
                    }
                else
                {
                    bw1.write(text.charAt(i));
                    res+=text.charAt(i);
                }
            }
            bw1.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
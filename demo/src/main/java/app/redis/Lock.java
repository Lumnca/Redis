package app.redis;

 public class  Lock {
    private  static int s = 0;
    private final static int max = 2;
    public static Boolean  tryLock(){
        if(s < max){
            s += 1;
            return true;
        }
        else{
            return false;
        }
    }
    public static void unLock(){
        if(s>0){
            s -= 1;
        }
    }
    public static int get(){
        return s;
    }
}

package  com.back_hexiang_studio.context;

public class UserContextHolder {

  private static final ThreadLocal<Long> userHolder = new ThreadLocal<>();// ThreadLocal<String>
  //设置当前用户
  public static void setCurrentId(Long user_id){
    userHolder.set(user_id);
  }
  //取到当前用户
  public static Long  getCurrentId(){
    return userHolder.get();
  }
  //移出
  public static void clear(){
    userHolder.remove();
  }

}
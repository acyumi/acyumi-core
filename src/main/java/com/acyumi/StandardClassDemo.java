package com.acyumi;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 代码规范的演示类
 * <p>
 * <p>
 * 注释相关： <br/>
 * 1、每个类都要放上相应的注释 <br/>
 * 2、且要有@author和@date信息 <br/>
 * 3、开发时要求在IDE中安装阿里的编码规约插件(idea和eclipse都有相应版本) <br/>
 * 3.1、阿里的编码规约插件在一些编码提示上可能与我们想要的不符合，可以针对些关掉不符合我们需求的提示<br/>
 * 4、类、成员变量和方法上的注释要求是javadoc注释， <br/>
 * &nbsp;(注意是以“/**”开头的，而不是以“/*”开头，javadoc注释使用的是html语法)， <br/>
 * &nbsp;需要特别说明的是，除了类、成员变量和方法上的说明注释使用javadoc注释之外， <br/>
 * &nbsp;其他地方禁止使用javadoc注释，包括： <br/>
 * &nbsp;&nbsp; 1)、方法体内部， <br/>
 * &nbsp;&nbsp; 2)、javadoc与javadoc之上的方法之间的空白处 <br/>
 * &nbsp;&nbsp; 3)、javadoc与javadoc之上的成员变量之间的空白处 <br/>
 * &nbsp;&nbsp; 4)、javadoc与import代码区域之间的空白处之间的空白处 <br/>
 * &nbsp;&nbsp; 5)、package语句上面的声明信息 <br/>
 * &nbsp;&nbsp; 6)、javadoc与javadoc之上的另外一个类之间的空白处 <br/>
 * &nbsp;而应该使用普通多行注释(以“/*”开头)和单行注释(“//”) <br/>
 * 另外，idea对注释这方面的实时检测力度远比eclipse强，代码优化提示丰富，建议都使用idea <br/>
 * (idea中，把光标放在类名、成员变量名、方法名上，然后使用快捷键Ctrl+Q就可以显示javadoc的效果， <br/>
 * 如果接口实现类上没有javadoc注释而接口上有，那么则继承显示接口上的注释内容)
 * <p>
 * <p>
 * 记住：代码一定要写注释，注释不怕多，就怕你不写，最好每个成员变量每个方法每个细节都写上注释，但要整理好，不要乱
 * <p>
 * <p>
 * 代码整理相关： <br/>
 * 1、统一使用四个空格代替一个tab缩进(IDE都可以设置) <br/>
 * 2、if语句和for循环语句等必须要有大括号包裹住代码块(不管代码块是否超过一行) <br/>
 * 3、每行尽量不超过120个字符(每个IDE都可以设置自动换行的值)，但为美观需要可以有些许行超过120个字符 <br/>
 * 4、每行代码之间根据代码逻辑应该有相应的空行，而不是全部压缩在一起没有空行 <br/>
 * 5、以对齐的方式注释代码，而不是在行的第一个字符的位置就开始写注释的关键字(IDE可设置) <br/>
 * <p>
 * <p>
 * 记住：代码一定要整理(idea -> Ctrl+Alt+L | eclipse -> Ctrl+Shift+F) <br/>
 * (因为不同IDE之间的全局整理效果不同，所以尽量先选中自己要整理的代码块再按快捷键整理)
 *
 * @author Mr.XiHui
 * @date 2018/9/28
 */
//@Component
public class StandardClassDemo {

    /*
     * 总体布局优先级
     * 1、成员变量
     * 2、构造方法
     * 3、public业务逻辑方法
     * 4、private方法
     * 5、get和set方法
     * 6、重写的Object方法(如equals(),toString())
     * */

    //总体说明
    //==============================================================================================
    //成员变量

    /*
     * 参考下面成员变量的布局，
     * 成员变量一共分为4层
     * 每层之间使用一行或两行空行
     * 使用javadoc多行注释
     *
     * 1、static且final(先写static再写final)
     * 2、static
     * 3、final
     * 4、非static且非final
     *
     * static且final的成员变量名要全部大写，单词之间用下划线分隔
     * 其他的成员变量全部都以小写开头的驼峰式命名
     * */

/*
 * 非对齐方式注释代码(不建议这样注释)
 */

//  非对齐方式注释代码(不建议这样注释)
//  private static final String PRIVATE_STATIC_FINAL_FIELD_DEMO0 = "DEMO0";

    //对齐的方式注释代码
    //private static final String PRIVATE_STATIC_FINAL_FIELD_DEMO0 = "DEMO0";
    // 对齐的方式注释代码
    // private static final String PRIVATE_STATIC_FINAL_FIELD_DEMO0 = "DEMO0";

    private static final String PRIVATE_STATIC_FINAL_FIELD_DEMO1 = "DEMO1";
    private static final int PRIVATE_STATIC_FINAL_FIELD_DEMO2 = 233;
    private static final Map<String, Integer> PRIVATE_STATIC_FINAL_FIELD_DEMO3 = new HashMap<>();

    private static String privateStaticFieldDemo1 = "DEMO2";
    private static String privateStaticFieldDemo2 = "DEMO2";

    private final String privateFinalFieldDemo1 = "DEMO3";
    /**
     * final的成员变量演示2
     * <p>
     * spring中注入的HttpServletRequest是单例的代理类，
     * 查看源码可知其最终获取的数据是通过ThreadLocal存放的，
     * 所以这样注入使用是线程安全的
     */
    private final HttpServletRequest request;


    /**
     * 非static且非final的成员变量演示1
     */
    private String privateFieldDemo1 = "DEMO4";
    /**
     * 非static且非final的成员变量演示2
     */
    private String privateFieldDemo2;

    //成员变量
    //==============================================================================================
    //构造方法

    /**
     * 构造方法
     * <p>
     * 不使用成员变量的注入方式，
     * 使用spring官方推荐的构造注入
     */
    //@org.springframework.beans.factory.annotation.Autowired
    public StandardClassDemo(HttpServletRequest request) {
        this.request = request;
    }

    //构造方法
    //==============================================================================================
    //public static方法

    public static int testPublicStatic() {
        return 0;
    }

    //public static方法
    //==============================================================================================
    //private static方法

    private static int testPrivateStatic() {
        return 0;
    }

    //private static方法
    //==============================================================================================
    //public业务逻辑方法

    /*
     * 业务代码以增删改查的顺序编写
     * 1、增(service和biz层使用add,到了dao层则使用insert)
     * 2、删
     * 3、改
     * 4、查
     * 如果是批量操作，则以batch开头，加动作，再加业务对象的复数
     * 如下面的batchAddPojos()
     * */

    /**
     * 添加pojo
     *
     * @return 成功添加的结果数
     */
    public int addPojo() {
        return 1;
    }

    // /**
    //  * 批量添加pojo
    //  *
    //  * @param pojos pojo列表
    //  * @return 成功添加的结果数
    //  */
    // public int batchAddPojos(List<Pojo> pojos) {
    //     return 1;
    // }

    //增
    //-------------------------------------------------------------------------
    //删

    public int deletePojo() {
        return 1;
    }

    public int batchDeletePojos() {
        return 1;
    }

    //删
    //-------------------------------------------------------------------------
    //改

    public int updatePojo() {
        return 1;
    }

    public int batchUpdatePojos() {
        return 1;
    }

    //改
    //-------------------------------------------------------------------------
    //查

    public int getPojo() {
        return 1;
    }

    /*public List<Pojo> batchGetPojos() {
        return Collections.emptyList();
    }*/

    //public业务逻辑方法
    //==============================================================================================
    //private方法

    private int doSomething() {
        return 1;
    }

    //private方法
    //==============================================================================================
    //get/set方法

    /*
     * 成员变量的get和set的方法放在一起
     * 先get再set
     * */

    public String getPrivateFieldDemo1() {
        return privateFieldDemo1;
    }

    public void setPrivateFieldDemo1(String privateFieldDemo1) {
        this.privateFieldDemo1 = privateFieldDemo1;
    }

    public String getPrivateFieldDemo2() {
        return privateFieldDemo2;
    }

    public void setPrivateFieldDemo2(String privateFieldDemo2) {
        this.privateFieldDemo2 = privateFieldDemo2;
    }

    //get/set方法
    //==============================================================================================
    //重写的Object方法

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}

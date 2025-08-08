import org.junit.Assert
import org.junit.Test

/**
 *
 * 在 groovy 中

 大括号是留给闭包（closure）使用的，数组、列表都使用中括号进行定义；
 数组、列表的元素都有序，可通过下标索引；
 区别：数组元素类型相同，列表元素类型可以不同。
 *
 * @author chenyilei
 *  2022/05/08 10:09
 */
class _3_列表_数组 {

    @Test
    public void arrayIndexOut() {
        def a = [1, 2, 3, 4]
        println(a[100]) // null
    }

    @Test
    void a1() {
        //error: java中定义数组
//        int[] array = {1, 2, 3};
        //groovy中定义数组
        int[] array = [1, 2, 3]
        String[] array2 = ["1", "2", "3"]
        String[] array3 = [[5, 6], "2", "3"]

        System.err.println(array2)
        System.err.println(array3)

        assert array3[0] == "[5, 6]"
        assert array3[0] instanceof String
    }

    @Test
    void 列表常用方法_指定了类型时会进行提前转换() {
        //定义的时候指定了类型, 则会在编译器进行强转,  例如 [5] 会变成字符串的 "[5]"
        def list1 = [1, 2]  // list<int>
        def list2 = [3, 4]
        String[] array3 = [[5], "2", "3"] //强转成了 "[5]" 字符串
        def array4 = [[5], "2", "3"]  //arrayList [serializable]

        Assert.assertEquals(array3[0], "[5]")
        assert ["[5]", "2", "3"] == array3

        //add是把参数作为一个元素进行添加，参数是集合时，会整个集合作为一个元素添加
        list1.add(list2)
        Assert.assertEquals([1, 2, [3, 4]], list1)

        //如果要将其它集合中的元素都添加到当前集合汇总，可以用 plus 或 + ，返回副本
        list2 = list2.plus([6, 6])
//        + [6, 6]
        Assert.assertEquals([3, 4, 6, 6], list2)

    }

    @Test
    public void 列表遍历() {
        def list = [6, "lisi", "wangwu"] as LinkedList  // 默认是arraylist
        assert list instanceof LinkedList

        //范围截取
        assert [6, "lisi"] == list[0..1]

        for (String a1 : list) {
            System.err.println(a1.getClass()) //这里 6被强转成了 "6"
        }
        System.err.println("=====")
        for (def a1 : list) {
            System.err.println(a1.getClass()) //class java.lang.Integer  这样就没有
        }
        System.err.println("=====")
        for (i in 0..<2) {
            def serializable = list[i]
            System.err.println(serializable.getClass().toString() + "   " + serializable.toString())
        }
        System.err.println("=====")
        list.forEach { tt -> tt.toString() }
        list.each { it.toString() } //默认 it 迭代器

    }

    /**
     * 元祖 tuple
     tuple是一种特殊的list，和list一样元素有序、类型可以不同，可以通过下标来访问。
     与list的区别：不能增删、修改元素（引用类型可以修改内部属性值，但不能修改引用本身），否则会抛出 UnsupportedOperationException。
     IDE代码提示中有 add()、remove() 之类的方法，但那是list的方法，tuple实际是不能使用的
     */
    @Test
    void 元组() {
        def tuple2 = new Tuple2<>(1, "2")

        println(tuple2[1] + tuple2.getV1())

        def list1 = [6]
        println(tuple2)

        list1.addAll(tuple2)
        println(list1) //[6, 1, 2]

        list1.add(tuple2)
        println(list1) // [6, 1, 2, [1, 2]]

        /**
         * class java.lang.Integer
         class java.lang.Integer
         class java.lang.String
         class groovy.lang.Tuple2
         */
        for (def t in list1) {
            println(t.getClass())
        }

    }

    @Test
    public void 范围() {
        def a = 1..<10

        println a.getFrom()
        println a.to

        for (def t in a) {

        }

        for (def t in 10) {

        }

    }

    @Test
    public void filter() {
        def strings = ["1", "2", "3"]
        def all = strings.findAll { it -> it == "2" }
        System.err.println(all)
    }

}

import org.junit.Assert
import org.junit.Test


/**
 * """  """  模板可以用 ${}替换一些常量
 * '''  ''' 则不行
 */
class _2_str_execute {
    public static void main(String[] args) {
        字符串模板();

        //字符串模板
        def s1 = "replace"
        def s2 = "1234 ${s1}"
        String s4 = "haha ${s2[1..2]}  ${s2[2..1]} " // 23
        println(s4 + '\n' + "*" * 3) //重复字符串数量

        println("=========")
        Process execute = "git help".execute() //Runtime
        println(execute.text.substring(0, 100))
    }


    def "public"() {
        println("关键词!!")
    }


    @Test
    void 字符串模板() {
        println('dddd单引号也可以成为字符串')

        def s1 = '''
    line1
    line2
    '''
        println s1

        def s2 = """
    l1 
    l2
"""
        println s2

    }

    @Test
    void split() {
        def list = splitStringToList("a,b,c", ",")
        Assert.assertEquals(3, list.size())
        println(list)
    }

    //将字符串分割成List, 考虑边界和异常情况
    List<String> splitStringToList(String str, String delimiter) {
        if (str == null || str.isEmpty()) {
            return []
        }
        if (delimiter == null || delimiter.isEmpty()) {
            throw new IllegalArgumentException("Delimiter cannot be null or empty")
        }
        return str.split(delimiter).collect { it.trim() }.findAll { !it.isEmpty() }
    }

}


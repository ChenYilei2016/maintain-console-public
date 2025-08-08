import org.junit.Test

/**
 * @author chenyilei
 *  2022/05/08 11:32
 */


class _4_map {

    @Test
    public void map_xml引擎() {
        def binding = [
                StudentName: 'Joe',
                id         : 1,
                subject    : 'Physics'
        ]
        def testMapTemplate = """
                     <name>${binding.StudentName}</name>
                     <ID>${binding.id}</ID>
                   """;

        println binding['id'] // 1
        println testMapTemplate

        def engine = new groovy.text.XmlTemplateEngine()
        def text = '''
               <document xmlns:gsp='http://groovy.codehaus.org/2005/gsp'>
                  <Student>
                     <name>${StudentName}</name>
                     <ID>${id}</ID>
                     <subject>${subject}</subject>
                  </Student>
               </document> ''';

        def render = engine.createTemplate(text).make(binding)
        println render
    }

    @Test
    public void map_diy() {
        def t = [
                "名称": ""
        ]

    }

    static class User {
        String name
        int id
    }

    @Test
    void map_业务收集_对象转换() {


        def a = [
                new User(name: '张三', id: 18),
                new User(name: '李四', id: 22),
        ]

        System.err.println(a)

        //获取所有id [18, 22]
        def collect = a.collect { it -> it.id }
        System.err.println(collect)

        //c1: [[it:18], [it:22]]
        def collect1 = collect.stream().map { it -> [it: it] }.collect()
        System.err.println("c1: " + collect1)
    }

}

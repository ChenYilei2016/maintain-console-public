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

}

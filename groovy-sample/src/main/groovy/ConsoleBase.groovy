/**
 * @author chenyilei
 *  2022/05/09 17:08
 */


class ConsoleBase {
    def storage = [:] //空map

    def propertyMissing(String name, value) { this.storage[name] = value }

    def propertyMissing(String name) { this.storage[name] }

}
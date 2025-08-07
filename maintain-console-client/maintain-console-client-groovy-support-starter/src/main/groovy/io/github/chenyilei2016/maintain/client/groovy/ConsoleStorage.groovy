package io.github.chenyilei2016.maintain.client.groovy
/**
 * @author chenyilei
 *  2024/05/09 17:08
 */

class ConsoleStorage {
    def storage = [:] //空map

    def propertyMissing(String name, value) { this.storage[name] = value }

    def propertyMissing(String name) { this.storage[name] }

}
package nl.topicuszorg.databroker.lrzawebservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LrzaWebserviceApplication

fun main(args: Array<String>)
{
    runApplication<LrzaWebserviceApplication>(*args)
}
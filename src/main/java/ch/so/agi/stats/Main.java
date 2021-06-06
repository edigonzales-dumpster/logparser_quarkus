package ch.so.agi.stats;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.transaction.Transactional;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Main implements QuarkusApplication {

    @ActivateRequestContext
    @Transactional
    @Override    
    public int run(String... args) throws Exception {

//        Fruit fruit = new Fruit();
//        fruit.name = "Apple";
//        fruit.persist();
        
        LogParser logparser = new LogParser();
        logparser.doImport("/Users/stefan/Downloads/api-gateway-logs/00-api-gateway-10-9f9kb.log");
        //logparser.doImport("/Users/stefan/Downloads/api-gateway-logs/01-api-gateway-10-nrfvc.log");

        

        return 0;
    }

}

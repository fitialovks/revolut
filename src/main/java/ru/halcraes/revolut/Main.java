package ru.halcraes.revolut;

import ru.halcraes.revolut.db.AccountService;
import ru.halcraes.revolut.db.Database;
import ru.halcraes.revolut.web.RestApi;

import static spark.Spark.init;
import static spark.Spark.port;

public class Main {
    public static void main(String[] args) {
        Database database = Database.initialize();
        AccountService accountService = new AccountService(database.getDataSource());
        RestApi api = new RestApi(accountService);
        port(8080);
        api.configure();
        init();
        // Spark does not have a nice API to wait for it to exit, so main thread ends here.
        // Good news is that H2 will not block shutdown.
    }
}

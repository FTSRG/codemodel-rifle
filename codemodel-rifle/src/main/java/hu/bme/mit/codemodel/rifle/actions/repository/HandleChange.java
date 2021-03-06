package hu.bme.mit.codemodel.rifle.actions.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import hu.bme.mit.codemodel.rifle.database.CsvAssembler;
import org.neo4j.driver.v1.Transaction;

import com.google.common.base.Stopwatch;
import com.shapesecurity.shift.ast.Module;
import com.shapesecurity.shift.parser.JsError;
import com.shapesecurity.shift.parser.ParserWithLocation;
import com.shapesecurity.shift.scope.GlobalScope;
import com.shapesecurity.shift.scope.ScopeAnalyzer;

import hu.bme.mit.codemodel.rifle.database.DbServices;
import hu.bme.mit.codemodel.rifle.database.DbServicesManager;
import hu.bme.mit.codemodel.rifle.database.ASTScopeProcessor;
import hu.bme.mit.codemodel.rifle.utils.ResourceReader;

public class HandleChange {
    private static final String SET_COMMIT_HASH = ResourceReader.query("setcommithash");
//    private static final String REMOVE_FILE = ResourceReader.query("removefile");

    private static final Logger logger = Logger.getLogger("codemodel");

    public boolean add(String sessionId, String path, String content, String branchId, String commitHash, Transaction tx) {
        setCommitHashInNewTransaction(branchId, commitHash);

        try {
            parseFile(sessionId, path, content, branchId, tx);

            return true;
        } catch (JsError jsError) {
            System.err.println(path);
            jsError.printStackTrace();

            return false;
        }
    }

    public boolean addCsv(CsvAssembler csvAssembler, String sessionId, String path, String content, String branchId, String commitHash) {
        try {
            this.parseFileCsv(csvAssembler, sessionId, path, content, branchId);

            return true;
        } catch (JsError jsError) {
            System.err.println(path);
            jsError.printStackTrace();

            return false;
        }
    }

    protected void parseFile(String sessionId, String path, String content, String branchId, Transaction tx) throws JsError {
        Stopwatch stopwatch = Stopwatch.createUnstarted();

        ParserWithLocation parser = new ParserWithLocation();
        stopwatch.start();
        Module module = parser.parseModule(content);
        long parseDone = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        logger.info(
            String.format("%s %s %dms", path, "PARSE", parseDone)
        );
        stopwatch.reset();

        stopwatch.start();
        GlobalScope scope = ScopeAnalyzer.analyze(module);
        long scopeDone = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        logger.info(
            String.format("%s %s %dms", path, "SCOPE", scopeDone)
        );
        stopwatch.reset();

        ASTScopeProcessor astScopeProcessor = new ASTScopeProcessor(path, parser);
        astScopeProcessor.processScope(scope, sessionId, tx);
    }

    protected void parseFileCsv(CsvAssembler csvAssembler, String sessionId, String path, String content, String branchId) throws JsError {
        Stopwatch stopwatch = Stopwatch.createUnstarted();

        ParserWithLocation parser = new ParserWithLocation();
        stopwatch.start();
        Module module = parser.parseModule(content);
        long parseDone = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        logger.info(
            String.format("%s %s %dms", path, "PARSE", parseDone)
        );
        stopwatch.reset();

        stopwatch.start();
        GlobalScope scope = ScopeAnalyzer.analyze(module);
        long scopeDone = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        logger.info(
            String.format("%s %s %dms", path, "SCOPE", scopeDone)
        );
        stopwatch.reset();

        ASTScopeProcessor astScopeProcessor = new ASTScopeProcessor(path, parser);
        astScopeProcessor.processScopeCsv(csvAssembler, scope, sessionId);
    }

//    public boolean modify(String sessionId, String path, String content, String branchId, String commitHash) {
//        remove(sessionId, path, branchId, commitHash);
//        return add(sessionId, path, content, branchId, commitHash);
//    }
//
//    public boolean remove(String sessionId, String path, String branchId, String commitHash) {
//        return removeFile(sessionId, path, branchId, commitHash);
//    }
//
//    protected boolean removeFile(String sessionId, String path, String branchId, String commitHash) {
//        Map<String, Object> parameters = new HashMap<>();
//        parameters.put("path", path);
//        parameters.put("sessionid", sessionId);
//
//        final DbServices dbServices = DbServicesManager.getDbServices(branchId);
//        try (Transaction tx = dbServices.beginTx()) {
//            setCommitHash(dbServices, tx, branchId, commitHash);
//            dbServices.execute(REMOVE_FILE, parameters);
//            tx.success();
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return false;
//    }

    private void setCommitHash(DbServices dbServices, Transaction tx, String branchId, String commitHash) {
        if (commitHash == null) {
            return;
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("commithash", commitHash);

        dbServices.execute(SET_COMMIT_HASH, parameters);
    }

    private void setCommitHashInNewTransaction(String branchId, String commitHash) {
        final DbServices dbServices = DbServicesManager.getDbServices(branchId);

        try (Transaction tx = dbServices.beginTx()) {
            setCommitHash(dbServices, tx, branchId, commitHash);
            tx.success();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

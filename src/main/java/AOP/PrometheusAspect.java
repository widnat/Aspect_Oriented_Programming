package AOP;

import io.prometheus.client.Gauge;
import io.prometheus.client.Summary;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

import io.prometheus.client.Counter;
import io.prometheus.client.exporter.HTTPServer;

import java.io.IOException;


@Aspect
public class PrometheusAspect {

    Counter numberOfIterations = Counter.build()
            .namespace("java")
            .name("number_of_iterations")
            .help("Counts the number of attempted inserts and removes")
            .register();
    Counter failedAdds = Counter.build()
            .namespace("java")
            .name("number_of_failed_adds")
            .help("Counts the number of failed adds")
            .register();
    Counter failedRemoves = Counter.build()
            .namespace("java")
            .name("number_of_failed_removes")
            .help("Counts the number of failed removes")
            .register();
    Gauge numberOfNodes = Gauge.build()
            .namespace("java")
            .name("number_of_nodes")
            .help("Counts Num Nodes")
            .register();
    Summary insertionTimer = Summary.build()
            .namespace("java")
            .name("time_to_add")
            .help("returns add time")
            .register();

  /**
   * This pointcut targets the serverOperation method in the AOP.Main package
   */
  @Pointcut("execution(* Main.serverOperation(..))")
  public void serverOperationExecution(){}

  /**
   * This pointcut targets the startThread Method in the AOP.Main.java
   */
  @Pointcut("execution(* Main.startThread(..))")
  public void startThreadPointcut(){}

    @Pointcut("execution(* BST.add(T))")
    public void addNode(){}

    @Pointcut("execution(* BST.remove(T))")
    public void removeNode(){}

  /**
   * This After Advice tells the numberOfIterations to increment after the serverOperation finishes
   * @param joinPoint Holds a reference to the method that was executed
   */
  @After("serverOperationExecution()")
  public void afterServerOperation(JoinPoint joinPoint) {
    numberOfIterations.inc();
  }


  /**
   * Starts the Prometheus Exporter Server
   * All prometheus data is viewable on localhost:SERVER_PORT/metrics if prometheus is running and configured to scrape data from your given server port
   *@param joinPoint Holds a reference to the method that was executed
   */
  @Before("startThreadPointcut()")
  public void afterThreadInitialization(JoinPoint joinPoint) {
    final int SERVER_PORT = 8080;
          try {
            HTTPServer server = new HTTPServer(SERVER_PORT);
            System.out.println("Prometheus exporter running on port " + SERVER_PORT);
        } catch (IOException e) {
            System.out.println("Prometheus exporter was unable to start");
            e.printStackTrace();
        }
  }

  //Add your other Advices here
    @Around("addNode()")
    public Object addToTree(ProceedingJoinPoint joinPoint) throws Throwable {
        Summary.Timer timer = insertionTimer.startTimer();

        Object returnObject = joinPoint.proceed();
        boolean added = (boolean) returnObject;

        if (added) {
            numberOfNodes.inc();
        }
        else {
            failedAdds.inc();
        }

        timer.observeDuration();

        return returnObject;
    }

    @Around("removeNode()")
    public Object removeFromTree(ProceedingJoinPoint joinPoint) throws Throwable {
        Object returnObject = null;

          try {
              returnObject = joinPoint.proceed();
              numberOfNodes.dec();
          }
          catch(FailedRemoveException e) {
              failedRemoves.inc();
          }

          return  returnObject;
    }
}

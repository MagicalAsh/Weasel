package us.magicalash.weasel.search.advice;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import us.magicalash.weasel.search.representation.SearchResponse;

@Aspect
public class TimingAdvice {

    @Pointcut("execution(us.magicalash.weasel.search.representation.SearchResponse+ *(*))")
    private void returnsSearchResponse() {}

    @Around("returnsSearchResponse()")
    public Object timeIt(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        SearchResponse response = (SearchResponse) joinPoint.proceed();
        long end = System.currentTimeMillis();

        response.setTook(end - start);

        return response;
    }

}

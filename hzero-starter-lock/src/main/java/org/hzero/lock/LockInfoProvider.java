package org.hzero.lock;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.hzero.lock.annotation.Lock;
import org.hzero.lock.annotation.LockKey;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

/**
 * 锁信息支持类
 *
 * @author xianzhi.chen@hand-china.com 2019年1月14日下午7:08:06
 */
public class LockInfoProvider {

    private ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    private ExpressionParser parser = new SpelExpressionParser();

    /**
     * 获取锁信息
     *
     * @param joinPoint
     * @param lock
     * @return
     */
    public LockInfo getLockInfo(JoinPoint joinPoint, Lock lock) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获取KEY
        List<String> keyList = this.getKeyList(joinPoint, lock);
        String lockName = getLockName(lock.name(), signature, keyList);
        long waitTime = getLockWaitTime(lock);
        long leaseTime = getLockLeaseTime(lock);
        TimeUnit timeUnit = getLockTimeUnit(lock);
        return new LockInfo(lockName, keyList, waitTime, leaseTime, timeUnit);
    }

    // 获取锁KEY名称
    private List<String> getKeyList(JoinPoint joinPoint, Lock lock) {
        Method method = getMethod(joinPoint);
        List<String> definitionKeys = getSpelDefinitionKey(lock.keys(), method, joinPoint.getArgs());
        List<String> keyList = new ArrayList<>(definitionKeys);
        List<String> parameterKeys = getParameterKey(method.getParameters(), joinPoint.getArgs());
        keyList.addAll(parameterKeys);
        return keyList;
    }

    // 获取拦截方法
    private Method getMethod(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (method.getDeclaringClass().isInterface()) {
            try {
                method = joinPoint.getTarget().getClass().getDeclaredMethod(signature.getName(),
                        method.getParameterTypes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return method;
    }

    // 获取方法定义KEY
    private List<String> getSpelDefinitionKey(String[] definitionKeys, Method method, Object[] parameterValues) {
        List<String> definitionKeyList = new ArrayList<>();
        for (String definitionKey : definitionKeys) {
            if (definitionKey != null && !definitionKey.isEmpty()) {
                EvaluationContext context =
                        new MethodBasedEvaluationContext(null, method, parameterValues, nameDiscoverer);
                Object value = parser.parseExpression(definitionKey).getValue(context);
                if (value != null) {
                    definitionKeyList.add(value.toString());
                }
            }
        }
        return definitionKeyList;
    }

    // 获取参数KEY
    private List<String> getParameterKey(Parameter[] parameters, Object[] parameterValues) {
        List<String> parameterKey = new ArrayList<>();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getAnnotation(LockKey.class) != null) {
                LockKey keyAnnotation = parameters[i].getAnnotation(LockKey.class);
                if (keyAnnotation.value().isEmpty()) {
                    parameterKey.add(parameterValues[i].toString());
                } else {
                    StandardEvaluationContext context = new StandardEvaluationContext(parameterValues[i]);
                    Object value = parser.parseExpression(keyAnnotation.value()).getValue(context);
                    if (value != null) {
                        parameterKey.add(value.toString());
                    }
                }
            }
        }
        return parameterKey;
    }

    // 获取锁名称
    private String getLockName(String annotationName, MethodSignature signature, List<String> keyList) {
        if (annotationName.isEmpty()) {
            return String.format("%s.%s.%s", signature.getDeclaringTypeName(), signature.getMethod().getName(),
                    StringUtils.collectionToDelimitedString(keyList, "", "-", ""));
        } else {
            return annotationName;
            // return String.format("%s.%s", annotationName, businessKeyName);
        }
    }

    // 获取锁等待时间
    private long getLockWaitTime(Lock lock) {
        return lock.waitTime();
    }

    // 获取锁默认释放时间
    private long getLockLeaseTime(Lock lock) {
        return lock.leaseTime();
    }

    // 获取锁默认时间单位
    private TimeUnit getLockTimeUnit(Lock lock) {
        return lock.timeUnit();
    }
}

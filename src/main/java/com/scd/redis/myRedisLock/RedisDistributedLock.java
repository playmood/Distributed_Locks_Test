package com.scd.redis.myRedisLock;

import cn.hutool.core.util.IdUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class RedisDistributedLock implements Lock {
    private StringRedisTemplate stringRedisTemplate;

    private String lockName;
    private String uuidValue;
    private long expireTime;

    public RedisDistributedLock(StringRedisTemplate stringRedisTemplate, String lockName) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.lockName = lockName;
        this.uuidValue = IdUtil.simpleUUID() + ":" + Thread.currentThread().getId();
        this.expireTime = 50L;
    }

    @Override
    public void lock() {
        tryLock();
    }

    @Override
    public boolean tryLock() {
        try{
            tryLock(-1L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        if (time == -1L){
            String script = "if redis.call('exists',KEYS[1]) == 0 or redis.call('hexists',KEYS[1],ARGV[1]) == 1 then " +
                    "  redis.call('hincrby',KEYS[1],ARGV[1],1) " +
                    "  redis.call('expire',KEYS[1],ARGV[2]) " +
                    "  return 1 " +
                    "else " +
                    "  return 0" +
                    "end";
            while(!stringRedisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(this.lockName), uuidValue, String.valueOf(expireTime))){
                // 暂停60ms
                try{
                    TimeUnit.MILLISECONDS.sleep(60);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    @Override
    public void unlock() {
        String script = "if redis.call('HEXISTS',KEYS[1],ARGV[1]) == 0 then return nil elseif redis.call('HINCRBY',KEYS[1],ARGV[1],-1) == 0 then return redis.call('del',KEYS[1]) else return 0 end";
        Long flag = stringRedisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(this.lockName), uuidValue, String.valueOf(expireTime));
        if (null == flag){
            throw new RuntimeException("this lock doesn't exists");
        }

    }


    @Override
    public Condition newCondition() {
        return null;
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }
}

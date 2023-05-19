package com.scd.redis.service;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@Service
@Slf4j
public class InventoryService
{
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Value("${server.port}")
    private String port;

    private Lock lock = new ReentrantLock();

    public String sale()
    {
        String retMessage = "";
        lock.lock();
        try
        {
            //1 查询库存信息
            String result = stringRedisTemplate.opsForValue().get("inventory001");
            //2 判断库存是否足够
            Integer inventoryNumber = result == null ? 0 : Integer.parseInt(result);
            //3 扣减库存
            if(inventoryNumber > 0) {
                stringRedisTemplate.opsForValue().set("inventory001",String.valueOf(--inventoryNumber));
                retMessage = "成功卖出一个商品，库存剩余: "+inventoryNumber;
                System.out.println(retMessage);
            }else{
                retMessage = "商品卖完了，o(╥﹏╥)o";
            }
        }finally {
            lock.unlock();
        }
        return retMessage+"\t"+"服务端口号："+port;
    }

    // 基本的分布式锁写法 setnx
    public String sale2(){
        String retMessage = "";
        String Key = "RedisLock";
        String uuidValue = IdUtil.simpleUUID() + ":" + Thread.currentThread().getId();

        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(Key, uuidValue);
        // 抢不到的线程要继续重试
        if (!flag){
            // 暂停20ms
            try{
                TimeUnit.MILLISECONDS.sleep(20);
            }catch(InterruptedException e){
                e.printStackTrace();
            }
            // 递归 容易导致栈溢出
            sale();
        }else{
            // 业务代码
            try
            {
                //1 查询库存信息
                String result = stringRedisTemplate.opsForValue().get("inventory001");
                //2 判断库存是否足够
                Integer inventoryNumber = result == null ? 0 : Integer.parseInt(result);
                //3 扣减库存
                if(inventoryNumber > 0) {
                    stringRedisTemplate.opsForValue().set("inventory001",String.valueOf(--inventoryNumber));
                    retMessage = "成功卖出一个商品，库存剩余: "+inventoryNumber;
                    System.out.println(retMessage);
                }else{
                    retMessage = "商品卖完了，o(╥﹏╥)o";
                }
            }finally {
                stringRedisTemplate.delete(Key);
            }
        }

        return retMessage + "\t" + "port: " + port;
    }

    public String sale21(){
        String retMessage = "";
        String Key = "RedisLock";
        String uuidValue = IdUtil.simpleUUID() + ":" + Thread.currentThread().getId();

        // 抢不到的线程要继续重试
        // while自旋 更加安全
        while (!stringRedisTemplate.opsForValue().setIfAbsent(Key, uuidValue)){
            // 暂停20ms
            try{
                TimeUnit.MILLISECONDS.sleep(20);
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
        try {
            //1 查询库存信息
            String result = stringRedisTemplate.opsForValue().get("inventory001");
            //2 判断库存是否足够
            Integer inventoryNumber = result == null ? 0 : Integer.parseInt(result);
            //3 扣减库存
            if(inventoryNumber > 0) {
                stringRedisTemplate.opsForValue().set("inventory001",String.valueOf(--inventoryNumber));
                retMessage = "成功卖出一个商品，库存剩余: "+inventoryNumber;
                System.out.println(retMessage);
            }else{
                retMessage = "商品卖完了，o(╥﹏╥)o";
            }
        }finally {
            stringRedisTemplate.delete(Key);
        }


        return retMessage + "\t" + "port: " + port;
    }

    public String sale3(){
        String retMessage = "";
        String Key = "RedisLock";
        String uuidValue = IdUtil.simpleUUID() + ":" + Thread.currentThread().getId();

        // 抢不到的线程要继续重试
        // while自旋 更加安全
        // 给锁添加过期时间，更加安全，防止业务挂了没有释放锁，操作需具备原子性
        // 但是 如果业务时间长 过期时间满足不了 可能会导致其他线程获得还没释放的锁
        while (Boolean.FALSE.equals(stringRedisTemplate.opsForValue().setIfAbsent(Key, uuidValue, 30L, TimeUnit.SECONDS))){
            // 暂停20ms
            try{
                TimeUnit.MILLISECONDS.sleep(20);
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
        // stringRedisTemplate.expire(Key, 30L, TimeUnit.SECONDS);
        try {
            //1 查询库存信息
            String result = stringRedisTemplate.opsForValue().get("inventory001");
            //2 判断库存是否足够
            Integer inventoryNumber = result == null ? 0 : Integer.parseInt(result);
            //3 扣减库存
            if(inventoryNumber > 0) {
                stringRedisTemplate.opsForValue().set("inventory001",String.valueOf(--inventoryNumber));
                retMessage = "成功卖出一个商品，库存剩余: "+inventoryNumber;
                System.out.println(retMessage);
            }else{
                retMessage = "商品卖完了，o(╥﹏╥)o";
            }
        }finally {
            stringRedisTemplate.delete(Key);
        }


        return retMessage + "\t" + "port: " + port;
    }
}
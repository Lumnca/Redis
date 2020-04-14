# :maple_leaf:rediscluster

<b id="t"></b>

:arrow_down:[集群配置](#a1)

:arrow_down:[集群配置命令](#a2)

尽管 Redis 的性能很好，但是有时候依旧满足不了应用的需要，比如过多的用户进入主页，导致 Redis 被频繁访问，此时就存在大量的读操作。

对于一些热门网站的某个时刻（比如促销商品的时候）每秒成千上万的请求是司空见惯的，这个时候大量的读操作就会到达 Redis 服务器，触发许许多多的操作，显然单靠一台 Redis 服务器是完全不够用的。

一些服务网站对安全性有较高的要求，当主服务器不能正常工作的时候，也需要从服务器代替原来的主服务器，作为灾备，以保证系统可以继续正常的工作。

因此更多的时候我们更希望可以读/写分离，读/写分离的前提是读操作远远比写操作频繁得多，如果把数据都存放在多台服务器上那么就可以从多台服务器中读取数据，从而消除了单台服务器的压力，读/写分离的技术已经广泛用于数据库中了。

**主从同步基础概念**

互联网系统一般是以主从架构为基础的，所谓主从架构设计的思路大概是：

`在多台数据服务器中，只有一台主服务器，而主服务器只负责写入数据，不负责让外部程序读取数据。`

`存在多台从服务器，从服务器不写入数据，只负责同步主服务器的数据，并让外部程序读取数据。`

`主服务器在写入数据后，即刻将写入数据的命令发送给从服务器，从而使得主从数据同步。`

`应用程序可以随机读取某一台从服务器的数据，这样就分摊了读数据的压力。`

`当从服务器不能工作的时候，整个系统将不受影响；当主服务器不能工作的时候，可以方便地从从服务器中选举一台来当主服务器。`


当然这不是固定的思路，每一种数据存储的软件都会根据其自身的特点对上面的这几点思路加以改造，但是万变不离其宗，只要理解了这几点就很好理解 Redis 的复制机制了。主从同步机制如下图所示。

![](http://c.biancheng.net/uploads/allimg/190725/5-1ZH5133K49E.png)

这个时候读数据就可以随机从服务器上读取，当从服务器是多台的时候，那么单台服务器的压力就大大降低了，这十分有利于系统性能的提高，当主服务器出现不能工作的情况时，也可以切换为其中的一台从服务器继续让系统稳定运行，所以也有利于系统运行的安全性。当然由于 Redis 自身具备的特点，所以其也有实现主从同步的特殊方式。


<p id="a1"></p>

### :fallen_leaf:集群配置

:arrow_double_up:[返回目录](#t)

**（1）集群原理**

在Redis集群中，所有的Redis节点彼此互联，节点内部使用二进制协议优化传输速度和带宽。当一个节点挂掉后，集群中超过半数的节点检测失效时才认为该节点已失效。不同于Tomcat集群需要使用反向代理服务器，Redis集群中的任意节点都可以直接和Java客户端连接。Redis集群上的数据分配则是采用哈希槽（HASHSLOT），Redis集群中内置了16384个哈希槽，当有数据需要存储时，Redis会首先使用CRC16算法对key进行计算，将计算获得的结果对16384取余，这样每一个key都会对应一个取值在0~16383之间的哈希槽，Redis则根据这个余数将该条数据存储到对应的Redis节点上，开发者可根据每个Redis实例的性能来调整每个Redis实例上哈希槽的分布范围。

**（2）集群规划**

本案例在同一台服务器上用不同的端口表示不同的Redis服务器（伪分布式集群）。

`主节点：47.106.254.86：8001，47.106.254.86：8002，47.106.254.86：8003。`

`从节点：47.106.254.86：8004，47.106.254.86：8005，47.106.254.86：8006。`

**（3）集群配置**

`Redis 集群管理工具redis-trib.rb依赖Ruby环境，首先需要安装Ruby环境，由于CentOS 7yum库中默认的Ruby版本较低，因此建议采用如下步骤进行安装。
首先安装RVM，RVM是一个命令行工具，可以提供一个便捷的多版本Ruby环境的管理和切换，安装命令如下：`

```
sudo yum install ruby  
gpg --keyserver hkp://keys.gnupg.net --recv-keys 409B6B1796C275462A1703113804BB82D39DC0E3 7D2BAF1CF37B13E2069D6956105BD0E739499BDB
curl -sSL https://get.rvm.io | bash -s stable
source /etc/profile.d/rvm.sh
```

然后就是列出安装表,安装一个比较稳定的版本：

```
rvm list known
rvm install 2.6.3
```

最后安装Redis依赖：

```
gem install redis
```

**集群配置**

首先在Redis中创建节点，由于Redis集群必须至少创建6个节点，接下来创建 redisCluster 文件夹，将前面下载的 Redis 压缩文件复制到 redisCluster 文件
夹中之后编译安装，操作命令如下：

```
mkdir redisCluster
cp-f./redis-4.0.10.tar.gz./ redisCluster/
cd redisCluster 
tar-zxvf redis-4.0.10.tar.gz 
cd redis-4.0.10
make MALLOC=libc
make install
```

安装成功后，将redis-4.0.10/src目录下的redis-trib.rb文件复制到redisCluster目录下，命令如下：

```
cp -f ./redis-4.0.10/src/redis-trib.rb  ./
```

然后在redisCluster目录下创建6个文件夹，分别命名为8001、8002、8003、8004、8005、8006，再将redis-4.0.10目录下的redis.conf文件分别往这6个目录中复制一份，然后对每个目录中的redis.conf文件进行修改，以8001目录下的redis.conf文件为例，主要修改如下配置

```
port 8001
#bind 127.0.0.1
cluster-enabled yes 
cluster-config-file nodes-8001.conf 
protected no 
daemonize yes 
requirepass 123456
masterauth 123456
```

这里的配置在6.1.2小节的单机版安装配置的基础上增加了几条，其中端口修改为8001，cluster-enabled 表示开启集群，cluster-config-file表示集群节点的配置文件，由于每个节点都开启了密码认证，因此又增加了masterauth配置，使得从机可以登录到主机上。按照这里的配置，对8002~8006目录中的redis.conf文件依次进行修改，注意修改时每个文件的port 和cluster-config-file不一样。全部修改完成后，进入redis-4.0.10目录下，分别启动6个Redis实例，相关命令如下：

```
redis-server ../8001/redis.conf
redis-server ../8002/redis.conf
redis-server ../8003/redis.conf
redis-server ../8004/redis.conf
redis-server ../8005/redis.conf
redis-server ../8006/redis.conf
```

当6个Redis实例都启动成功后，回到redisCluster目录下，首先对redis-trib.rb文件进行修改，由于配置了密码登录，而该命令在执行时默认没有密码，因此将登录不上各个Redis实例，此时用vi编辑器打开redis-trib.rb文件，搜索到如下一行：

`=Redis.new（：host =>@info[：host]，：port =>Qinfo[：port]，：timeout =>60）`

修改这一行，添加密码参数：

`=Redis.new（：host =>einfo[：host]，：port =>@info[：port]，：timeout=>60，：password=>"123456"）`

123456就是各个Redis实例的登录密码。

这些配置都完成后，接下来就可以创建Redis集群了。执行如下命令创建Redis集群：

```
./redis-trib.rb create--replicas 1 192.168.248.144:8001 192.168.248.144:8002 192.168.248.144:8003 192.168.248.144:8004 192.168.248.144：8005 192.168.248.144:8006
```

192.168.248.144改为你的IP地址

其中，replicas表示每个主节点的slave数量。在集群的创建过程中会分配主机和从机，每个集群在创建过程中都将分配到一个唯一的id并分配到一段slot。
当集群创建成功后，进入redis-4.0.10目录中，登录任意Redis实例，命令如下：

```
redis-cli -p 8001- a 123456 -c
```

p表示要登录的集群的端口，-a表示要登录的集群的密码，-c则表示以集群的方式登录。登录成功后，通过cluster info命令可以查询集群状态信息（如图6-5所示），通过`cluster nodes`命令可以查询集群节点信息，在集群节点信息中，可以看到每一个节点的id，该节点是slave还是master，如果是slave，那么它的master的id是什么，如果是master，那么每一个master的slot范围是多少，这些信息都会显示出来。

查看节点是否运行：

```
ps -ef  | grep redis
```

![](https://github.com/Lumnca/Spring-Boot/blob/master/img/a45.png)

如上显示表示成功！ 可以输如命令 `redis-cli -c -p 8001 -a "123456" cluster nodes` 查看节点信息：


```
5d8888e618099cf24788b5fbbf04dba24b05c100 47.106.254.86:8005@18005 master - 0 1585294164000 8 connected 0-5460
e563d6cf1372dd8983a5ba702e3b5d5095f135e2 127.0.0.1:8002@18002 master - 0 1585294164393 0 connected
f1bb25d04b1adb4916d679e321e21c7d798696c8 47.106.254.86:8004@18004 master - 0 1585294163393 11 connected 10923-16383
8849017747d3e1ae36c68f3ef924865d89300298 47.106.254.86:8003@18003 slave f1bb25d04b1adb4916d679e321e21c7d798696c8 0 1585294163000 11 connected
48f80b60bdfa5dd221e8ea1c04dc70e002fbec5c 47.106.254.86:8006@18006 master - 0 1585294163000 12 connected 5461-10922
6a846196fdcf1c3276c0ff78afb0e2e97411eda4 127.0.0.1:8001@18001 myself,slave 5d8888e618099cf24788b5fbbf04dba24b05c100 0 1585294162000 1 connected
```

其中第一列就是服务的id号，第二列是地址与口号，第三列说明了是主节点，还是从节点，后面就是分配的slot等信息。下面来介绍一下常用的命令

***



<p id="a2"></p>

### :fallen_leaf:集群配置命令

:arrow_double_up:[返回目录](#t)


当集群创建成功后，随着业务的增长，有可能需要添加主节点，添加主节点需要先构建主节点实例，将redisCluster目录下的8001目录再复制一份，名为8007，根据第3步的集群配置修改8007目录下的redis.conf文件，修改完成后，在redis-4.0.10目录下运行如下命令启动该节点：

`redis-server ../8007/redis.conf`

整个过程如图：

![](https://github.com/Lumnca/Redis/blob/master/img/a15.png)

启动成功后，进入redisCluster目录下，执行如下命令将该节点添加到集群中：

`./redis-trib.rb add-node 127.0.0.1:8007 127.0.0.1:8001`

如无错误提示表示成功！


可以看到，新实例已经被添加进集群中，但是由于slot已经被之前的实例分配完了，新添加的实例没有slot，也就意味着新添加的实例没有存储数据的机会：

```
1e324ceeb66d21e798da96323bb6202515ce7c1a 127.0.0.1:8007@18007 master - 0 1585298288221 0 connected  [这里没有分配值]
59427e2d47df77debb474342a4a4b3cdf97a3856 47.106.254.86:8003@18003 master - 0 1585298293233 3 connected 10923-16383
f2364ef8ff08658ba5f87be05d0983b8eb37aaf7 127.0.0.1:8001@18001 myself,master - 0 1585298290000 1 connected 0-5460
4bb9d4f07f64b37f82d65659b18a4259b1952d2f 47.106.254.86:8006@18006 slave 2c2c48734f8ad11aece7c951f00f47875908bdf5 0 1585298292231 6 connected
714f63e21a9af7dfaba108fde52e623e7a2854da 47.106.254.86:8004@18004 slave 59427e2d47df77debb474342a4a4b3cdf97a3856 0 1585298290227 4 connected
e51e32c4815c3199d3fc25a04e9d5be6beafd507 47.106.254.86:8005@18005 slave f2364ef8ff08658ba5f87be05d0983b8eb37aaf7 0 1585298293000 1 connected
2c2c48734f8ad11aece7c951f00f47875908bdf5 47.106.254.86:8002@18002 master - 0 1585298292000 2 connected 5461-10922
```

此时需要从另外三个实例中拿出一部分slot分配给新实例。具体操作如下。首先，在redisCluster目录下执行如下命令对slot重新分配：

`./redis-trib.rb reshard 127.0.0.1:8001`

接下来会进行分配选择：

![](https://github.com/Lumnca/Redis/blob/master/img/a16.png)

第一个配置是要拿出多少个slot分配给新实例，本案例配置了2000个。

第二个是把拿出来的1000个slot分配给谁，输入接收这2000个slot的Redis实例的id，这个id在节点添加成功后就可以看到，也可以进入集群控制台后利用cluster nodes命令查看。

第三个配置是这2000个slot由哪个实例出，例如从端口为8001的实例中拿出2000个slot分配给端口为8007的实例，那么这里输入8001的id后按回车键，再输入done按回车键即可，如果想将2000个slot均摊到原有的所有实例中，那么这里输入all按回车键即可。

slot分配成功后，再查看节点信息，就可以看到新实例也有slot了：

![](https://github.com/Lumnca/Redis/blob/master/img/a17.png)

由于是均分的slot，所以8007得到的是断续的点。

上面添加的节点是主节点，从节点的添加相对要容易一些。添加从节点的步骤如下：首先将redisCluster目录下的8001目录复制一份，命名为8008，然后按照上面中8007的配置修改8008目录下的redis.conf，修改完成后，启动该实例，然后输入如下命令添加从节点：

`./redis-trib.rb add-node --slave --master-id 
e0f2751b46c9ed3ca130e9fc825540386feaafb2 192.168.248.144:8008 192.168.248.144：8001`

添加从节点需要指定该从节点的masterid，-master-id后面的参数即表示该从节点master的id，192.168.248.144：8008表示从节点的地址，192.168.248.144：8001则表示集群中任意一个实例的地址。

当从节点添加成功后，登录集群中任意一个Redis实例，通过cluster nodes命令就可以看到从节点的信息。

如果删除的是一个从节点，直接运行如下命令即可删除：`./redis-trib.rb del-node 192.168.248.144：8001 122b2098df746afc3a77beddaad85630bf75ab9a`中间的实例地址表示集群中的任意一个实例，最后的参数表示要删除节点的id。但若删除的节点占有slot，则会删除失败，此时按照上面分配空间slot提到的办法，先将要删除节点的slot全部都分配出去，然后运行如上命令就可以成功删除一个占有slot的节点了。

当你用停用一个节点时可以通过`kill -9 pid`来关闭这个进程。pid可以通过`ps -ef | grep redis`来查询该pid。

如果你是要关闭所有服务，或者删除集群，需要把目录下的节点数据文件xxxxnode.conf，aof,rdb,文件删除，并再登录到节点上去执行`flushdb`命令才会完全删除，并且不会在你下次重新配置的时候出现节点不为空的错误。





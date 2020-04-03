# Redis缓存技术开发 #

:arrow_down:[Redis缓存简介](#a1)

:arrow_down:[Redis缓存使用](#a2)

:arrow_down:[Redis缓存常见问题](#a3)

<p id="a1"></p>

### Redis缓存简介 ###

redis大多时候都是作为缓存来整合到项目中的。为什么要使用缓存呢？我们在大多数访问web网站无非是查询一些数据，比如逛淘宝买东西，学校选课的时候。这个时候你会发觉逛淘宝不会怎么卡，但是相对于学校选课来说，你能很明显的感觉的加载缓慢，甚至服务崩溃（当然也有些做的好的学校:sweat_smile:） 。这是由于服务端开发设计模式不同所导致的，像淘宝这样的大型公司做的服务端，用了多个服务器实现了**负载均衡，数据库读写分离，微服务，缓存等多种技术**才到达了千万级的并发服务。

然而对于学校相信大多数学校但是单机服务，也就是服务部署，数据库部署都是在同一台机器上，而且服务器数量也只有一台或者几台工作。像这样的不做其他优化最大也就支持几百的并发量，但是选课的时候并发量估计是不止几百的，进而导致服务器超负荷工作最终导致服务崩溃。

当然学校不可能像大公司那样拥有那么全面的技术以及成千上万的服务器来支持并发服务，但是可以使用缓存技术来提高并发量，我们这里就来简单来介绍缓存技术。

首先我们都知道缓存是存在内存中的，所以它的读写速度是非常快的，这里就可以和数据库读写做对比，数据库的内容是持久化到文件中了的，也就是外存，显然这种读写速度是比不上内存的，所以使用缓存可以让更多的访问落在读取内存上，这种读取比访问数据库是快很多的。而且每读取一次数据库，就得建立一次数据库连接，执行命令，返回结果，断开数据连接。这个过程是非常消耗资源的，如果访问的人过多，就会建立多个连接，如果不释放这些连接，最后会导致访问资源耗尽。看一下下面几个例子：

一个普通的数据库访问API所耗时：

![as](https://github.com/Lumnca/Redis/blob/master/img/a2.png)

这是一个从数据库中查询一本书的数据接口， 可以看到响应速度只有几ms，这取决于数据库的数据数量和接口实现的业务逻辑。我这里的数据只有1800条所以看不到太大的延迟，但是我们稍微提升了一下并发量在100左右

![](https://github.com/Lumnca/Redis/blob/master/img/a3.png)

可见大多数访问延时都有几百ms，比单个访问也高了一百倍左右。如果继续提升并发量，更能看出这个响应延迟。

接下来我们看下缓存的访问时延，在已经有了缓存数据的情况下：

![](https://github.com/Lumnca/Redis/blob/master/img/a4.png)

可以看到普遍都是几ms的延迟，即使增加并发量所用的延迟依然是几ms，这是由于这个接口的数据所要返回的数据已被写入缓存，我们来看下这个接口的缓存逻辑：

```java
  @Cacheable(value = "c1")
    public String getBookById(Integer id){
        System.out.println("==================GetBookByID("+id+")==================");
        return JSON.toJSONString(bookDao.getOne(id));
    }
```

这是spring boot所集成的redis功能，其中@Cacheable(value = "c1")是使用缓存，只要这个方法第一次被访问就会被写入缓存，由于缓存是key-value型的，这里就会把方法参数作为key值，其方法返回值的内容作为value值。并设置了缓存过期的时间，在这段时间内，如果访问这个方法的参数在缓存中存在，那么这个步骤将不会被再次执行，其中的打印语句System.out.println也不会再次执行。

所以像这样所有被访问的接口参数都会被写入缓存，下一次再次访问就从内存中读取，而不走数据库，因此提高了访问速度。

****

<p id="a2"></p>

### Redis缓存使用 ###

首先先使用spring boot整合redis 具体配置步骤参见这篇文章[spring boot整合Redis](https://github.com/Lumnca/Spring-Boot/blob/master/Spring%20Boot%E6%95%B4%E5%90%88NoSql.md)

然后再来使用缓存，参见[Redis缓存](https://github.com/Lumnca/Spring-Boot/blob/master/%E7%BC%93%E5%AD%98.md)

关于代码介绍上面这里面都有介绍。这里不做说明。

:arrow_down:[Redis缓存常见问题](#a3)

<p id="a3"></p>

### Redis缓存常见问题 ###

**:one:redis的持久化机制**

我们知道redis的数据都是存储在内容中，那么redis实例挂了或者重启了怎么办？那是不是我们的数据就全没了？其实redis提供了持久化来解决这类问题。首先来看下这个过程：

首先我们添加了一个name值为lumnca，接下来我们将这个redis服务进程关闭

![](https://github.com/Lumnca/Redis/blob/master/img/a6.png)

再次启动redis，查看值：

![](https://github.com/Lumnca/Redis/blob/master/img/a7.png)

可见依然存在这是为什么呢？其实是redis通过**RDB和AOF机制**来实现了持久化。

***RDB持久化***机制原理是对redis中的数据执行者周期性的持久化，是redis默认的持久化机制，他的默认持久化策略是如果在900S之内执行了一次数据变更操作，或者在300S内执行了10次，再或者在60s执行了10000次，那么在这个900S、300S、60S将会生成一个redis数据快照文件。

***AOF持久化***机制对每条redis的写入指令都会做一条日志，以append-only的模式写入一个日志文件中，在redis重启的时候会回放日志文件中的指令来重新构建数据，它默认我没记错的话会每秒保存一个文件。

如果同时启用了RDB和AOF的话，redis会使用AOF来恢复数据，因为AOF的数据会更加完整。在我们实际生产使用中，可以定期把RDB和AOF做备份到云服务器来做灾难恢复，数据恢复。


我们可以在redis目录中查看这两个文件dump.rdb,appendonly.aof ：

![](https://github.com/Lumnca/Redis/blob/master/img/a9.png)

这两个文件可以在redis.conf配置文件中查看存放位置,我们尝试文本模式打开这两个文件：

rdb文件

![](https://github.com/Lumnca/Redis/blob/master/img/a8.png)

aof文件

![](https://github.com/Lumnca/Redis/blob/master/img/a10.png)

在rdb文件中可见我们的数据键值对，由于是由文本文件打开，所以导致有些乱码。而在aof中则记录了我们使用的redis指令。

然而这个记录文件并不是实时更新的，我们在进行添加其他数据或者操作后，它不一定会自动记录。对于RDB有两种触发方式，分别是自动触发和手动触发。

**自动触发：**

![](https://github.com/Lumnca/Redis/blob/master/img/a11.png)

**save m n**: 就是上面所说的在300S内执行了10次，再或者在60s执行了10000次，那么在这个900S、300S、60S将会生成一个redis数据快照文件，这是默认的，你也可以自己修改 m n这两个参数。

当然如果你只是用Redis的缓存功能，不需要持久化，那么你可以注释掉所有的 save 行来停用保存功能。可以直接一个空字符串来实现停用：save ""

**stop-writes-on-bgsave-error** ：默认值为yes。当启用了RDB且最后一次后台保存数据失败，Redis是否停止接收数据。这会让用户意识到数据没有正确持久化到磁盘上，否则没有人会注意到灾难发生了。如果Redis重启了，那么又可以重新开始接收数据了

**rdbcompression** ；默认值是yes。对于存储到磁盘中的快照，可以设置是否进行压缩存储。如果是的话，redis会采用LZF算法进行压缩。如果你不想消耗CPU来进行压缩的话，可以设置为关闭此功能，但是存储在磁盘上的快照会比较大。

**rdbchecksum** ：默认值是yes。在存储快照后，我们还可以让redis使用CRC64算法来进行数据校验，但是这样做会增加大约10%的性能消耗，如果希望获取到最大的性能提升，可以关闭此功能。

**dbfilename** ：设置快照的文件名，默认是 dump.rdb

**dir**：设置快照文件的存放路径，这个配置项一定是个目录，而不能是文件名。默认是和当前配置文件保存在同一目录。也就是说通过在配置文件中配置的 save 方式，当实际操作满足该配置形式时就会进行 RDB 持久化，将当前的内存快照保存在 dir 配置的目录中，文件名由配置的 dbfilename 决定。

**手动触发**

手动触发Redis进行RDB持久化的命令有两种：

1、save

该命令会阻塞当前Redis服务器，执行save命令期间，Redis不能处理其他命令，直到RDB过程完成为止。

显然该命令对于内存比较大的实例会造成长时间阻塞，这是致命的缺陷，为了解决此问题，Redis提供了第二种方式。

2、bgsave

执行该命令时，Redis会在后台异步进行快照操作，快照同时还可以响应客户端请求。具体操作是Redis进程执行fork操作创建子进程，RDB持久化过程由子进程负责，完成后自动结束。阻塞只发生在fork阶段，一般时间很短。

基本上 Redis 内部所有的RDB操作都是采用 bgsave 命令。

*执行 flushall 命令，也会产生dump.rdb文件，但里面是空的.*

**RDB优缺点**

RDB机制有几个优点，首先它会生成多个数据文件，每个文件都代表某个时刻redis中的数据快照，这种文件非常适合我们做冷备，可以定期把这种文件推送到云服务做存储来应对灾难恢复；其次，RDB对redis对外提供的读写服务影响比较小，可以让redis保持高性能，因为redis只需要fork一个子进程，让子进程执行磁盘IO操作来进行RDB持久化；而且由于RDB是基于数据文件恢复的，所以说相对于AOF来说更加快速。

RDB的缺点在于2个方面，首先在redis故障的时候，RDB由于它是定期去数据快照，所以说可能会丢失一部分数据；其次RDB每次在fork子进程来生成快照文件的时候，如果数据文件特别大，可能会导致对客户端提供服务暂停数毫秒，甚至数秒。

**AOF开启与配置**

在redis.conf配置文件中也包含了aof文件的配置，如下所示：

![](https://github.com/Lumnca/Redis/blob/master/img/a12.png)

`appendonly`：默认值为no，也就是说**redis 默认使用的是rdb方式持久化，如果想要开启 AOF 持久化方式，需要将 appendonly 修改为 yes**。

`appendfilename` ：aof文件名，默认是"appendonly.aof"

`appendfsync`：aof持久化策略的配置；no表示不执行fsync，由操作系统保证数据同步到磁盘，速度最快，但是不太安全；always表示每次写入都执行fsync，以保证数据同步到磁盘，效率很低；everysec表示每秒执行一次fsync，可能会导致丢失这1s数据。通常选择 everysec ，兼顾安全性和效率。

`no-appendfsync-on-rewrite`：在aof重写或者写入rdb文件的时候，会执行大量IO，此时对于everysec和always的aof模式来说，执行fsync会造成阻塞过长时间，no-appendfsync-on-rewrite字段设置为默认设置为no。如果对延迟要求很高的应用，这个字段可以设置为yes，否则还是设置为no，这样对持久化特性来说这是更安全的选择。  设置为yes表示rewrite期间对新写操作不fsync,暂时存在内存中,等rewrite完成后再写入，默认为no，建议yes。Linux的默认fsync策略是30秒。可能丢失30秒数据。默认值为no。

`auto-aof-rewrite-percentage`：默认值为100。aof自动重写配置，当目前aof文件大小超过上一次重写的aof文件大小的百分之多少进行重写，即当aof文件增长到一定大小的时候，Redis能够调用bgrewriteaof对日志文件进行重写。当前AOF文件大小是上次日志重写得到AOF文件大小的二倍（设置为100）时，自动启动新的日志重写过程。

`auto-aof-rewrite-min-size`：64mb。设置允许重写的最小aof文件大小，避免了达到约定百分比但尺寸仍然很小的情况还要重写。

`aof-load-truncated`：aof文件可能在尾部是不完整的，当redis启动的时候，aof文件的数据被载入内存。重启可能发生在redis所在的主机操作系统宕机后，尤其在ext4文件系统没有加上data=ordered选项，出现这种现象  redis宕机或者异常终止不会造成尾部不完整现象，可以选择让redis退出，或者导入尽可能多的数据。如果选择的是yes，当截断的aof文件被导入的时候，会自动发布一个log给客户端然后load。如果是no，**用户必须手动redis-check-aof修复AOF文件才可以**。默认值为 yes。


想要开启aof持久化，只需要将 appendonly的值no 修改为 yes即可。 重启 Redis 之后就会进行 AOF 文件的载入。异常修复命令：`redis-check-aof --fix` 进行修复


由于AOF持久化是Redis不断将写命令记录到 AOF 文件中，随着Redis不断的进行，AOF 的文件会越来越大，文件越大，占用服务器内存越大以及 AOF 恢复要求时间越长。为了解决这个问题，Redis新增了重写机制，当AOF文件的大小超过所设定的阈值时，Redis就会启动AOF文件的内容压缩，只保留可以恢复数据的最小指令集。可以使用命令 `bgrewriteaof` 来重写。

![](https://github.com/Lumnca/Redis/blob/master/img/a13.png)

**AOF优缺点**

AOF机制的优点：

* 1.AOF可以更好的避免数据丢失问题，默认AOF每隔1S，通过后台线程执行一次fsync操作，它最多丢失1S的数据。
* 2.AOF日志文件以append-only模式写入，所以没有任何磁盘寻址的开销，写入性能比较高，而且文件不容易受损坏，redis也提供了修复受损坏AOF文件的方式，很容易修复。
* 3.AOF日志文件可读性比较高，可以追踪误操作记录（如flush all），来删除这个日志记录进行紧急恢复。

AOF机制的缺点：
* 1.对于同一份数据来说，AOF日志文件通常比RDB数据快照文件更大。
* 2.AOF开启之后，支持的写QPS会比RDB支持的写QPS低，因为默认AOF会每秒去fsync一次日志。

**RDB和AOF该如何选择?**

其实我们应该两者一起使用的，用**AOF来保证尽可能的不丢失数据**，作为数据恢复的第一选择，**利用RDB来做冷备**，当AOF丢失或者不可用的时候来使用RDB快照来快速回复。如果单一选择RDB的话对比AOF可能会丢失部分数据，选择AOF的换又没有RDB回复的速度快而且AOF的备份很复杂。(经过验证，在Redis 5.0.5版本中，如果同时开启RDB和AOF进行持久化，在重启Redis时，只会加载AOF文件!!!)

***

**:two:redis的cluster模式**

虽然缓存有着极大的优点，但是也面临着和普通服务器一样的问题，就是万一缓存服务挂掉了，那岂不是就没有了缓存服务了？当然为了服务的高可用，redis缓存也有集群模式，也就是由很多redis服务一起来作用于缓存服务，一个服务挂掉了之后呢，并不会有太大的影响。其他服务节点会继续维持服务。cluster模式就是其中一种。由多个服务节点组成（至少6个，3从3主）一个主节点对应一个或多个从节点，主节点提供数据存取，从节点则是从主节点拉取数据备份，当这个主节点挂掉后，就会有这个从节点选取一个来充当主节点，从而保证集群不会挂掉。

**简介**

redis的cluster模式自动将数据分片，每个master上放部分数据，每个master可以挂slave节点。redis的cluster模式提供了一定程度的可用性，当某些节点发生故障或者无法通信时，集群能够继续正常运行。

**存储**

redis-cluster采用的分布式存储算法为hash slot，redis-cluster有固定的16384个hash slot对每个key计算CRC16值，然后对16384取模，就可以获取key对应的hash slot。redis-cluster会把hash-slot分散到各个master上，这样每一个master都会持有部分数据，当增加或者删除一个master，只需要移动hash slot则可，hash slot的移动成本很低。

如下图所示我们在上面添加数据，都会被随机分散到不同节节点上去，并且每当你要访问这个数据时，会跳转至改节点：

![](https://github.com/Lumnca/Redis/blob/master/img/a14.png)


如果客户端想要把某些数据存储到同一个hash slot，可以通过api的hash tag来实现。

**gossip协议**

gossip协议包含多种消息，包括ping，pong，meet，fail，等等。

* meet：某个节点发送meet给新加入的节点，让新节点加入集群中，然后新节点就会开始与其他节点进行通信，其实内部就是发送了一个gossip meet消息，给新加入的节点，通知那个节点去加入我们的集群。

* ping：每个节点都会频繁给其他节点发送ping，其中包含自己的状态还有自己维护的集群元数据，互相通过ping交换元数据，每个节点每秒都会频繁发送ping给其他的集群，ping，频繁的互相之间交换数据，互相进行元数据的更新。

* pong：返回ping和meet，包含自己的状态和其他信息，也可以用于信息广播和更新。

* fail：某个节点判断另一个节点fail之后，就发送fail给其他节点，通知其他节点，指定的节点宕机了。

**高可用性和主备切换原理**

一个节点在cluster-node-timeout内没有收到某一个节点的pong，那么它就会认为这个节点pfail了，它会在ping给其他节点，当超过半数节点都认为某个节点pfail了，那么那个节点就fail了。

对宕机的master node，redis cluster会从其所有的slave node中，选择一个切换成master node。它首先会检查每个slave node和master node断开连接的时间，如果超过了cluster-node-timeout * cluster-slave-validity-factor，那么这个slave node就没有资格成为master node。每个从节点，都根据自己对master复制数据的offset来设置一个选举时间，offset越大代表从master节点上复制的数据越多，选举时间越靠前，优先进行选举。当选举开始时，所有的master node给要进行选举的slave node进行投票，如果大部分都投票给了某个节点，那么选择通过，这个从节点或切换成master。

***

**:three:缓存雪崩和穿透**

缓存雪崩是发生在高并发的情况下，如果redis宕机不可用时`大量的请求涌入数据库`，导致数据库崩溃以至于整个系统不可用，在这种情况下数据库会直接无法重启，因为起来会被再次打挂。所以要想避免缓存雪崩可以考虑采用以下措施，首先尽量`保证redis的高可用`（可以通过`redis cluster`来提供高可用），其次可以使用一些限流降级组件（例如hystrix）来避免mysql被打挂，最后如果不是分布式系统可以考虑本地缓存一些数据。

缓存穿透发生在一些恶意攻击下，在有些`恶意攻击中会伪造请求来访问服务器`，由于伪造的错误的数据在redis中不能找到，所以请求直接打到了数据库，导致了高并发直接把mysql打挂，同缓存雪崩一样，在这种情况下即使重启数据库也不能解决问题。想避免这种问题可以考虑采用以下措施，首先对请求过来的参数做合法性校验，例如用户id不是负数；其次，可以考虑如果有参数频繁的访问数据库而不能查询到，可以在redis给它搞个空值，避免请求直接打在数据库。

下面呢我们简单列举一下这个过程和解决方案：

如下是一个简单的缓存功能服务类：


```java
@Service
@CacheConfig(cacheNames = "c1",cacheManager = "redisCacheManager")
    public class BookServer {

    @Autowired
    BookDao bookDao; 
    
    //获取缓存，如果值不存在则写入缓存
    @Cacheable(value = "c1")
    public String getBookById(Integer id){
        System.out.println("==================GetBookByID("+id+")==================");
        return JSON.toJSONString(bookDao.getOne(id));
    }
}
```

添加控制器，以供访问：

```java
    @Autowired
    BookServer bookServer;
    @GetMapping("getBook")
    public String getBookId(String id) {
        return bookServer.getBookById(Integer.parseInt(id));
    }
```

为了能够直观看到结果，我们可以定义一个切面：

```java
@Component
@Aspect
public class RedisAspect {
    @Pointcut("execution(* app.redis.*.*(..))")
    public void aop1(){

    }
    @Before(value = "aop1()")
    public void before(JoinPoint jp){
        System.out.println(jp.getSignature().getName()+"方法开始执行！参数为:"+ JSON.toJSONString(jp.getArgs()));
    }
    @After(value = "aop1()")
    public void after(JoinPoint jp){
        System.out.println(jp.getSignature().getName()+"方法执行结束！");
    }

}
```

运行spring boot我这里绑定的端口是8081：`http://127.0.0.1:8081/getBook?id=2` （也可以使用postman）

多次访问这个url达到多次访问可以看到控制台输出：

![](https://github.com/Lumnca/Redis/blob/master/img/a18.png)

可见我们一共访问了两次接口，但是只有第一次执行了数据库操作。后面的没有执行数据库操作。

接下来模拟集群redis服务不可用的情况：

单个从节点挂掉,要在项目运行下关闭节点，否在关掉节点再启动项目会报错！

![](https://github.com/Lumnca/Redis/blob/master/img/a19.png)

然后继续访问接口发现服务可用！说明单个从节点不会影响服务。挂掉主节点：

首先查看一下现在的节点情况：

![](https://github.com/Lumnca/Redis/blob/master/img/a20.png)

可见是如下的关系：

```
主节点 ： 8002 ， 8003 8004
主节点对应的从节点  8006 -> 8003 , 8005 -> 8002 , 8001 -> 8004
```
现在我们关闭 8002 主节点进程：

![](https://github.com/Lumnca/Redis/blob/master/img/a21.png)

然后再次访问刚才的url，发现服务报错

![](https://github.com/Lumnca/Redis/blob/master/img/a22.png)

这是因为其中一个主节点断开而导致的连接超时被自动断开所导致原因。但是前面不是说过从节点可以代替主节点工作吗？是的，这当然会，但是这需要一段时间来让redis集群中节点完成判断其中有主节点断开了，当半数的节点都ping不到这个节点时，就会实施从节点替换主节点。好了，等过一段现在我们再来看下节点情况：

![](https://github.com/Lumnca/Redis/blob/master/img/a23.png)

如上可见节点2已经无法工作了，而他对应的8005节点成了新的主节点！。再次运行服务，发现服务又可用了！

![](https://github.com/Lumnca/Redis/blob/master/img/a24.png)

我们再把8002端口服务开启，发现它不再是主节点了，而是8005的从节点！

![](https://github.com/Lumnca/Redis/blob/master/img/a25.png)

或许你有个疑问。那就是在从节点代替主节点这段时间里面访问的人岂不是会得不到想要信息，是的。所以我们这里要在这段时间里面处理用户的请求：

捕捉异常：

```java
    @GetMapping("getBook/{id}")
    public String getBookById(@PathVariable("id")  Integer id) {
        String book;
        try {
            book = bookServer.getBookById(id);
        }
        //也可以直接捕捉对应的错误
        catch (Exception e){
            //直接从数据库获取
            ......
        }
        return book;
    }
 ```


所以问题就从这里来了，本身检查连接超时就会花费一些时间，加上再从数据库获取数据，这个过程会需要一定的时间。如果这段时间涌入了许多并发访问，只要访问量够大，就会导致mysql服务崩溃。所以这样做会有问题。那有什么解决方案呢？

**本地缓存**

可以使用Ehcache 本地缓存来代替redis缓存不可用的时候：

配置本地缓存前参考前面的配置redis缓存的文章，这里不做演示。

添加本地缓存：

```java
@Service
@CacheConfig(cacheNames = "book_cache",cacheManager = "eCacheCacheManager")
public class BookServer2 {
    @Autowired
    BookDao bookDao;
    @Cacheable(value = "book_cache")
    public String getBookById(Integer id){
        System.out.println("=============从本地缓存写入数据===============");
        return JSON.toJSONString( bookDao.getOne(id));
    }
}
```

Redis缓存：

```java
@Service
@CacheConfig(cacheNames = "c1",cacheManager = "redisCacheManager")
public class BookServer {
    @Autowired
    BookDao bookDao;
    @Cacheable(value = "c1")
    public String getBookById(Integer id){
        System.out.println("==================从redis缓存写入数据==================");
        return JSON.toJSONString(bookDao.getOne(id));
    }
}

```

修改控制器接口：

```java
    @GetMapping("getBook")
    public String getBook2ById(@PathVariable("id")  Integer id) {
        String book;
        try {
            //redis获取
            book = bookServer.getBookById(id);
        }
        catch (Exception e){
            //本地获取
            book = bookServer2.getBookById(id);
        }
        return book;
    }
```

运行程序，再第一次访问后关闭一个主节点。再次访问：


![](https://github.com/Lumnca/Redis/blob/master/img/a26.png)

可见在这个期间使用的是本地缓存。`使用本地缓存作为二级缓存`是一个比较好的选择，即使redis缓存服务全部崩溃，本地缓存照样可以继续维持缓存服务。

还一种方式是使用`限流降级组件hystrix`。

**hystrix**

Hystix是由Netlix开源的一个延迟和容错库，用于隔离访问远程系统、服务或者第三方库，防止级联失败，从而提升系统的可用性与容错性。Hysix主要通过以下几点实现 延迟和容错。

* 包裹请求：使用HystrixCommand（或HystrixObservableCommand）包裹对依赖的调用逻辑，每个命令在独立线程中执行。这使用到了设计模式中的“命令模式”。

* 跳闸机制：当某服务的错误率超过一定阈值时，Hystrix可以自动或者手动跳闸，停止请求该服务一段时间。

* 资源隔离：Hystrix为每个依赖都维护了一个小型的线程池（或者信号量）。如果该线程池已满，发往该依赖的请求就被立即拒绝，而不是排队等候，从而加速失败判定。

* 监控：Hystrix可以近乎实时地监控运行指标和配置的变化，例如成功、失败、超时、以及被拒绝的请求等。

* 回退机制：当请求失败、超时、被拒绝，或当断路器打开时，执行回退逻辑。回退逻辑可由开发人员自行提供，例如返回一个缺省值。

* 自我修复：断路器打开一段时间后，会自动进入“半开”状态。

有关hystrix组件使用可参考这篇文章[使用Hystrix实现容错处理](https://github.com/Lumnca/Spring-Cloud/blob/master/%E4%BD%BF%E7%94%A8Hystrix%E5%AE%9E%E7%8E%B0%E5%BE%AE%E6%9C%8D%E5%8A%A1%E7%9A%84%E5%AE%B9%E9%94%99%E5%A4%84%E7%90%86.md)

下面我们来简单的使用Hystrix，在前面的例子中我们说过当一个主节点断掉后，我们再次尝试去访问时会有一个连接超时的等待时间，一般在5s以上。如果不做处理，有些用户可能以为服务无响应了，这时我们可以采用Hystrix设置响应超时时间执行回退：

```java
    @GetMapping("getBook")
    @HystrixCommand(defaultFallback = "fallback",commandProperties = {
            @HystrixProperty(name="execution.isolation.thread.timeoutInMilliseconds", value = "5000"),   //最大响应时间5000ms
    })
    public String getBookById(String id) {
        String book;
        try {
             book = bookServer.getBookById(Integer.parseInt(id));
        }
        catch (Exception e){
            book = bookServer2.getBookById(Integer.parseInt(id));
        }
        return book;
    }
    //执行的回退方法
    public String fallback(){
        return "请稍后再访问！";
    }
```

![](https://github.com/Lumnca/Redis/blob/master/img/a27.png)

当然我们最好不样这样做，这是由于不止连接超时会导致访问时间延长，像网络信号不好，访问量多，都会到导致响应时间延迟。有回退机制我们甚至可以不用捕捉异常：

```java
    @GetMapping("getBook/{id}")
    @HystrixCommand(fallbackMethod = "fallback")
    public String getBookById(@PathVariable("id")Integer id) {
        return bookServer.getBookById(id);
    }
    //回退方法：
    public String fallback(@PathVariable("id")Integer id){
        System.out.println("出现了异常！");
        return bookServer2.getBookById(id);
    }
```


断开其中一个主节点，我们使用JMeter模拟多个并发：

![](https://github.com/Lumnca/Redis/blob/master/img/a28.png)

可见所有的请求都是成功了返回的数据，而控制台中也捕捉了这些问题，并交给了本地缓存去做处理：

![](https://github.com/Lumnca/Redis/blob/master/img/a29.png)

所以我们可以不用像前面那样的去捕捉异常然后处理。当然这里并不是限流，如果想限制某个时间点并发量，进而保护服务进程不被打挂，可以像如下设置：

```java
@GetMapping("getBook/{id}")
    @HystrixCommand(fallbackMethod = "fallback",commandProperties = {
            @HystrixProperty(name="execution.isolation.strategy", value = "SEMAPHORE"),
            //同时的并发量不能超过10个
            @HystrixProperty(name="fallback.isolation.semaphore.maxConcurrentRequests", value = "10")
    })
    public String getBookById(@PathVariable("id")Integer id) {
        return bookServer.getBookById(id);
    }
    public String fallback(@PathVariable("id")Integer id){
        System.out.println("出现了异常！");
        return bookServer2.getBookById(id);
    }
```


然后使用Jmeter同时请求50个并发：

![](https://github.com/Lumnca/Redis/blob/master/img/a30.png)

然后我们观察控制台输出：

![](https://github.com/Lumnca/Redis/blob/master/img/a31.png)


到这里为止我们一直说的都是redis缓存服务不可用的情况下，但是在服务可用的情况下，依然会存在缓存雪崩的情况。例如在缓存中不存在的值同一时间接收到了大量访问，由于程序是异步执行的，当然不可能所有请求都能命中缓存，如果一大部分命中数据库而导致数据返回慢，当压力过大时会导致存储层直接挂掉，整个系统都受影响。所以我们还要解决这个问题。

首先看下在缓存中没有该数据的情况下：

```java
    @GetMapping("getBook/{id}")
    public String getBookById(@PathVariable("id")Integer id) {
        return bookServer.getBookById(id);
    }
```

同时模拟20个并发：

![](https://github.com/Lumnca/Redis/blob/master/img/a32.png)

查看控制台输出：

![](https://github.com/Lumnca/Redis/blob/master/img/a33.png)

我们可以看到在缓存没有数据值得时候20个并发访问中大部分的访问都是直接命中数据库的。而我们期望的是他们之间应该只有1个是命中了数据库的，然后写入缓存，剩下的都应该直接从缓存中取数据。那该怎么做呢？

**加锁控制**

 我们可以在对数据库查询的地方进行加锁控制，让该资源只允许一个使用，不要让所有请求都过去，这样可以保证存储服务不挂掉。下面列出最简单方式：
 
 
 
 ```java
    @GetMapping("getBook/{id}")
    public String getBookById(@PathVariable("id")Integer id) {
        String book;
        if(stringRedisTemplate.hasKey("sang:"+id)){
            System.out.println("**********从缓存中读取数据**********");
            return stringRedisTemplate.opsForValue().get("sang:"+id);
        }
        synchronized (this){
            book = bookServer.getBookById(id);
        }
        return book;
    }
 ```
 
 
 同样添加20个并发，观察控制台输出：
 
 ![](https://github.com/Lumnca/Redis/blob/master/img/a35.png)
 
 可见只有一个进入了数据库访问。其余的都是从缓存中获取值。


上面是最简单的方式，直接用代码同步块来加锁， 当然我们也可以用 Lock 来加锁，加锁的本质还是控制并发量，不要让所有请求瞬时压到数据库上面去，加了锁就意味着性能要丢失一部分。其实我们可以用信号量来做，就是限制并发而已，信号量可以让多个线程同时操作，只要在数据库能够抗住的范围内即可。

**分布式锁**

加锁除了用 Jvm 提供的锁，我们还可以用分布式锁来解决缓存雪崩的问题，分布式锁常用的有两种，`基于 Redis 和 Zookeeper 的实现`。当你从网上搜分布式锁的时候，出来一大堆实现的文章，不建议自己去实现这种功能，用开源的会好点，在这里介绍一个基于 Redis 实现的分布式锁。

Redisson 是一个在 Redis 的基础上实现的 Java 驻内存数据网格（In-Memory Data Grid）。

Redisson 不仅提供了一系列的分布式的 Java 常用对象，还提供了许多分布式服务（包括 BitSet、Set、Multimap、SortedSet、Map、List、Queue、BlockingQueue、Deque、BlockingDeque、Semaphore、Lock、AtomicLong、CountDownLatch、Publish/Subscribe、Bloom filter、Remote service、Spring cache、Executor service、Live Object service、Scheduler service）。

Redisson 提供了使用 Redis 最简单和便捷的方法。Redisson 的宗旨是促进使用者对 Redis 的关注分离（Separation of Concern），从而让使用者能够将精力更集中地放在处理业务逻辑上。

Redisson 跟 Jedis 差不多，都是用来操作 Redis 的框架，Redisson 中提供了很多封装，有信号量、布隆过滤器等。分布式锁只是其中一个，感兴趣的可以自行深入研究。


```java
RLock lock = redisson.getLock("anyLock");
// 最常见的使用方法 lock.lock();
// 支持过期解锁功能
// 10 秒钟以后自动解锁
// 无须调用 unlock 方法手动解锁
lock.lock(10, TimeUnit.SECONDS);
// 尝试加锁, 最多等待 100 秒, 上锁以后 10 秒自动解锁
boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
...
lock.unlock();
```

可以如下设计：

```java
      try {
           //尝试去获取锁
            if(lock.tryLock(10,10,TimeUnit.SECONDS)){
            
                //执行数据可操作
                ......
               
                //释放锁
                lock.unlock();
            }
            else{
                //等待或者其他处理
                ......
            }
        }
        catch (Exception e){

        }
```

当然了上面都是技术性的解决方案，最后呢还有设计方面的解决方案：

前面所说当缓存值不存在时，而那个时间段刚好又有大量访问就有可能导致缓存雪崩！因而我们的缓存过期时间要设置好。尽量避开大规模失效。统一去规划有效期，让失效时间分布均匀即可。

对于一些热门数据的持续读取，这种缓存数据也可以采取定时更新的方式来刷新缓存，避免自动失效。当然特殊情况下也可以设置永久生效。

**小结**

综合上面所有情况，总结出可以以下解决方案：

* 1）缓存存储高可用。比如 Redis 集群，这样就能防止某台 Redis 挂掉之后所有缓存丢失导致的雪崩问题。

* 2）缓存失效时间要设计好。不同的数据有不同的有效期，尽量保证不要在同一时间失效，统一去规划有效期，让失效时间分布均匀即可。

* 3）对于一些热门数据的持续读取，这种缓存数据也可以采取定时更新的方式来刷新缓存，避免自动失效。

* 4）服务限流和接口限流。如果服务和接口都有限流机制，就算缓存全部失效了，但是请求的总量是有限制的，可以在承受范围之内，这样短时间内系统响应慢点，但不至于挂掉，影响整个系统。

* 5）从数据库获取缓存需要的数据时加锁控制，本地锁或者分布式锁都可以。当所有请求都不能命中缓存，这就是我们之前讲的缓存穿透，这时候要去数据库中查询，如果同时并发的量大，也是会导致雪崩的发生，我们可以在对数据库查询的地方进行加锁控制，不要让所有请求都过去，这样可以保证存储服务不挂掉。

***

**:four:缓存与数据库双写一致性解决方案**

（1）`Cache Aside Pattern原则`

读的时候，先读缓存，缓存没有的话，那么就读数据库，然后取出数据后放入缓存，同时返回响应。更新的时候，先删除缓存，然后再更新数据库。

（2）`解决方案`

方案一：队列串行化，当更新数据的时候，根据数据的唯一标识可以经过hash分发后搞到一个jvm内部队列，读取数据的时候，如果发现数据不在缓存中，那么将重新读取+更新缓存的操作也根据唯一标识发送到同一个jvm内部的队列，可以判断一下队列中是否已经有查询更新缓存的操作，如果有直接把更新缓存操作取消掉，然后每个队列单线程消费。但是这种方案有几个问题需要根据业务或者测试去完善优化，首先多实例服务怎么把请求根据数据的唯一标识路由到同一个实例；其次，读请求长阻塞、请求吞吐量、热点问题这些可能需要大量的压力测试和业务处理。

方案二：分布式锁，当读数据的时候如果缓存miss，可以去尝试根据唯一标识（例如userId）获取锁，如果获取不到直接从数据库查询数据返回即可，不更新缓存，反之则更新缓存，之后释放锁。当写请求过来时要保证获取到公平锁或者获取锁失败可以直接拒绝（公平性获取锁可以通过zookeeper的临时顺序节点来实现），在更新完数据库可以同时更新缓存（也可以不更新）。


如果你是使用spring boot作为项目的框架的话，那么这个解决问题只需要一个注解即可：


```java
   //根据Id更新
    @CachePut(key = "#book.id")
    public String updateBookById(Book book){
        bookDao.save(book);
        System.out.println("==================updateBook=====================");
        return JSON.toJSONString(book);
    }
    //根据Id删除缓存
    @CacheEvict(key = "#id")
    public String deleteBookById(Integer id){
        System.out.println("==================deleteBook=====================");
        return "delete";
    }
```

控制器修改：

```java
    @GetMapping("getBook/{id}")
    public String getBookById(@PathVariable("id")Integer id) {
        String book;
        if(stringRedisTemplate.hasKey("sang:"+id)){
            return bookServer.getBookById(id);
        }
        synchronized (this){
            book = bookServer.getBookById(id);
        }
        return book;
    }
    @PostMapping("updatebook")
    public String updateBookById(@RequestBody Book book){
        return bookServer.updateBookById(book);
    }
    @DeleteMapping("deleteBookById/{id}")
    public String deleteBookBuId(@PathVariable("id") Integer id){
        return bookServer.deleteBookById(id);
    }
```

接下来使用postman做接口测试：

首先访问id为2的数据:

 ![](https://github.com/Lumnca/Redis/blob/master/img/a36.png)
 
 修改名称和价格：
 
![](https://github.com/Lumnca/Redis/blob/master/img/a37.png)
  
再次查询：
  
![](https://github.com/Lumnca/Redis/blob/master/img/a38.png)

我们发现缓存数据是刷新了的，最后删除缓存数据：

![](https://github.com/Lumnca/Redis/blob/master/img/a39.png)

在redis缓存中查询该数据：

![](https://github.com/Lumnca/Redis/blob/master/img/a40.png)

可见并没有该数据。


**:five:热点数据**

由于一个key是热点key必然会有大量的并发访问量，在这个key过期时，这是会有大量的线程去构建缓存，而构建缓存可能会很复杂，这样就导致了后端负载过大，这可能会导致系统崩溃。解决方式可以考虑以下几种之一，首先可以使用互斥锁，可以只让一个线程去构建，其他线程等待即可，这个已经在上面的加锁机制已经介绍过了；其次可以考虑设置redis key的过期时间。方案总结如下：

1.快过期的时候重置时间：

```java
    @GetMapping("getBook/{id}")
    public String getBookById(@PathVariable("id")Integer id) {
        if(stringRedisTemplate.hasKey("键值")){
          //当有效时间小于一小时
          if(stringRedisTemplate.getExpire("键值",TimeUnit.SECONDS)<3600){
              //重置为一天
              stringRedisTemplate.expire("键值",86400,TimeUnit.SECONDS);
          }
        }
        return  bookServer.getBookById(id);
    }
```

这样设置其实只有在最后期限间有人访问才能重置时间。如果这个期限设置的得当的话，可以一定程度上避免失效。

2.按照访问频率依次增加有效时间：

```java
    @GetMapping("getBook/{id}")
    public String getBookById(@PathVariable("id")Integer id) {
 
        if(stringRedisTemplate.hasKey("键值")){
            Long time = stringRedisTemplate.getExpire("键值"+id,TimeUnit.SECONDS);
            //每访问依次增加1个小时的有效期
            stringRedisTemplate.expire("键值",time+3600,TimeUnit.SECONDS);
        }
        return bookServer.getBookById(id);
    }
```

这样设置的话可以让热点数据时间在一段时间内达到不过期的效果。

3.设置永远不过期。

```java
   @GetMapping("getBook/{id}")
    public String getBookById(@PathVariable("id")Integer id) {
        //-1 永久
        stringRedisTemplate.expire("键值",-1,TimeUnit.SECONDS);
        
        return bookServer.getBookById(id);
    }
```


虽然这个方法是万无一失的。但是过多的永久数据会导致缓存空间不够用。因为它们会一直占有那个缓存空间，而且对性能也不是很好。所以不能为所有的
值做永久缓存，只能是热点数据。但是对于未来的热点数据是谁都无法判断的。这就需要我们人为的去寻找热点数据。设立数据监控，数据浏览统计，提前调查等方式
来找到可能的热点数据。





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



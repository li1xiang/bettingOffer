# There are two ways to provide an HttpServer
1. create() and create(InetSocketAddress addr, int backlog);  we choice the second method to create HttpServer. we can see from the picture,default backlog is 50, I search from the internet the linux default backlog is 128. so I set that to 100.![img_1.png](img_1.png)
2. Because high-concurrent request,so we can't use default thread pool. it's cpu resource so set core and max is cpu of number    +1
# About GET /<customerid>/session
1. Because high-concurrent request and the session is stored in memory, so I use ConcurrentHashMap to store session and add lock.
2. Get user session from ConcurrentHashMap, and check the session is expired, if not expired then return session and reset login time.
3. if not exist session we create a new session and store it in ConcurrentHashMap.
# ABOUT POST /<betofferid>/stake?sessionkey=<sessionkey>
1. Get userInfo from ConcurrentHashMap(loginMap),  and check userInfo is existed and the session is expired and remove it. 

# Sequence
![img.png](img.png)
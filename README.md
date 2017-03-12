[![](https://jitpack.io/v/mostroverkhov/firebase-data-rxjava.svg)](https://jitpack.io/#mostroverkhov/firebase-data-rxjava)

# firebase-data-rxjava

Simplifies firebase interaction for both client and server by providing set
of convenience read/write operations with reactive interface (RxJava1)

`FirebaseDataBaseManager` is library entry point

####Read operations
Reads are using `DataQuery`. DataQuery determines data window into database
  `DataQuery` is built using `DataQuery.Builder`

#####3 interaction models are supported:

1. `Data Window` backpressured stream of data windows `List<T>`. There is no window change notifications
2. `Notifications` non-backpressured stream of database entries `ChildChangeEvent<T>`, 
  interleaved with exactly one item for next window query
3. `Data window with notifications` backpressured stream of data windows `List<T>`, 
   with non-backpressured notifications stream for those windows 
   
####Write operations
API mirrors native firebase one, with 3 operations defined:

1. `setValue`
2. `updateChildren`
3. `removeValue`

####Artifacts
Artifacts are available through jitpack

#####Maven
```
   <repositories>
  		<repository>
	     <id>jitpack.io</id>
   		 <url>https://jitpack.io</url>
		 </repository>
  </repositories>
```
```
<dependency>
	    <groupId>com.github.mostroverkhov.firebase-data-rxjava</groupId>
	    <artifactId>rx</artifactId>
	    <version>0.1.8</version>
	</dependency>
  ```
  
#####Gradle
```
  repositories {
			maven { url 'https://jitpack.io' }
		}
```
```
    dependencies {
	        compile 'com.github.mostroverkhov.firebase-data-rxjava:rx:0.1.8'
	}
```

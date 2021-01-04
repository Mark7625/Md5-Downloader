# Cache Downloder

The library allows you to update your cache files as and when are they needed. This eliminates the need to download the whole client's cache every single time there is a cache update. This also allows you to update your cache without having to update the client meaning if you need to push out a simple sprite fix or a model fix you are able to without pushing out a new Client version.

# Setup

```kt
repositories {
	maven { url 'https://jitpack.io' }
}

dependencies {
	      implementation 'com.github.Mark7625:Md5-Downloader:1.0'
}
```

# Webserver

1) Make a empty Directory
2) Inside that make a file called cache.php and write the following code

![alt text](https://cdn.discordapp.com/attachments/772182417531732038/795361999353413693/unknown.png "")

3) Upload all your files that you want the user to download
4) Make the master hash

Once you have all your files you want to be updated in your cache go to where you call the down loader and edit the second boolean to true and run this will make a file called online_hashes.json in your root client folder once this is done just upload this to the same place as your cache.php and your done

Your files will now start to update when edited. To remake the hash just follow the 3rd step again when u want to push a update.


# Usage Kotlin

Basic

```kt
CacheDownloader(locationtocache, "urltowebhost/cache/", updateCheck = true, writeOnlineHash = false)
```

Advanced
```kt
CacheDownloader(locationtocache, "urltowebhost/cache/", updateCheck = true, writeOnlineHash = false,object: Progress() {
  override fun update(progress: Int, message: String) {
      drawLoadingbar(progress,message)
    }
 })
```

# Usage Java
Basic

```kt
new CacheDownloader(locationtocache, "urltowebhost/cache/", true,  false,null);
```

Advanced
```kt
new CacheDownloader(locationtocache,"urltowebhost/cache/", true,  false, (p, m) -> {
 drawLoadingbar(p,m)
});
```



const dbName = "fillerdb";
const storeName = "fillerstore"

var request = indexedDB.open(dbName, 2);

request.onerror = function(event) {
  // Handle errors.
};
request.onupgradeneeded = function(event) {

  var db = event.target.result;

  var objectStore = db.createObjectStore(storeName, { keyPath: "ssn" });


  objectStore.transaction.oncomplete = function(event) {
    console.log("store created")
  };
};


function quota(){
  navigator.storage.estimate().then(x=>console.log(x,"quota", x.quota/10e8,"usage",x.usage/10e8))                                
}

function fill(chunk=100, cb=x=>{}){

  var b = 9872108/200; // measured
  navigator.storage.estimate().then(x => {
    var avail = x.quota - x.usage
    var n = Math.floor(avail/(b*chunk)/2)+1
    console.log("avail", avail, "it", n)
    var request = indexedDB.open(dbName, 2);
    request.onerror = console.error
    var q;

    request.onsuccess = e => {
      var t = new Date().getTime()
      var db = e.target.result;
      var customerObjectStore = db.transaction(storeName, "readwrite").objectStore(storeName);
      var i = 0;
      var f = _ => {
        if (i==n){
          cb()
          return 
        }
        i++;

        console.log(i, "/", n)
        var customer = { ssn: Math.random().toString(36), name: "x".repeat(chunk*1024*1024) }
        var r = customerObjectStore.add(customer);
        r.onerror = e => {
          console.error(e)
          cb()
        }
        r.onsuccess = _ => {
          navigator.storage.estimate().then(q2 => {
            console.log(q2)
            if (q){
              console.log("diff:",q2.usage - q.usage)
              console.log("avail:",q2.quota - q2.usage)
              if(q2.usage - q.usage>q2.quota - q2.usage){
                console.error("gonna fail")
              }
            }
            q = q2;
          })
          f()
        }
      }
      f()
    }
  })
}
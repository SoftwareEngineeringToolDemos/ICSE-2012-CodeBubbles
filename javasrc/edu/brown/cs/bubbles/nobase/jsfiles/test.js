

function Cls() {
   this.x = 1;
   this.y = 2;
   this.first = function() { return this.x; }
}

Cls.prototype.second = function() { return this.y; }

var cx = new Cls();

console.log("Cls",typeof Cls,Cls);
console.log("Cls.prototype",typeof Cls.prototype,Cls.prototype);
console.log("cx",typeof cx,cx);

for (var x in cx) {
   console.log("x",x,cx[x]);
 }

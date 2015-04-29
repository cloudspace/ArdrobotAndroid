width = 68;
length = 30;

sBucketWidth = 26.5;
sBucketLength = 61.5;
sBucketDepth = 45;

p1 = [length/2, 63, 0];
p2 = [length/2, 5, 0];

bl = [11,10,0]; 
br = [86,10,0]; 
tl = [10,58,0]; 
tr = [91.5,58,0]; 

center = [length/2, width/2];
thickness = 2.5;
innerRadius = 1;
outerRadius = innerRadius + thickness;

union() {
    translate(p1) cylinder(h=6,r=3.9);
    translate(p2) cylinder(h=6,r=3.9);

    translate(p2) cylinder(h=13,r=2.4);
    translate(p1) cylinder(h=13,r=2.4);
    
    
    difference() {
        union() {
            linear_extrude(height = thickness) square([length,width]);
            difference() {
                translate([center[0] - sBucketLength / 2, center[1] - sBucketWidth / 2]) 
                linear_extrude(height = thickness) square([sBucketLength,sBucketWidth]);
            }
        }
        translate([center[0] - sBucketLength / 2 + 10 , center[1] - sBucketWidth / 2 + 2.6]) 
        linear_extrude(height = thickness)  square([sBucketLength - 20, sBucketWidth - 5.2]);                               
    }
}
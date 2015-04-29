width = 68;
length = 102.5;

p1 = [length/2, 63, 0];
p2 = [length/2, 5, 0];

c1 = [10, 10];
c2 = [length/2 + 10, 10];

bl = [11,10,0]; 
br = [86,10,0]; 
tl = [10,58,0]; 
tr = [91.5,58,0]; 
 
thickness = 3;
inner_radius = 1.5;
outer_radius = inner_radius + thickness;

union() {
    translate(p1) cylinder(h=6,r=3.9);
    translate(p2) cylinder(h=6,r=3.9);

    translate(p2) cylinder(h=13,r=2.4);
    translate(p1) cylinder(h=13,r=2.4);
    
    screw_hole([br[0], br[1]]);
    screw_hole([tl[0], tl[1]]);
    screw_hole([tr[0], tr[1]]);
    screw_hole([bl[0], bl[1]]);

    //screw hole base
    difference() {    
        translate([tl[0] - 5, tl[1] - 5]) linear_extrude(height = 1.3) square(10);
        translate([tl[0], tl[1]]) cylinder(h=5,r1=inner_radius,r2=inner_radius);
    }  

    difference() {    
        translate([bl[0] - 5, bl[1] - 5]) linear_extrude(height = 1.3) square(10);
        translate([bl[0], bl[1]]) cylinder(h=5,r1=inner_radius,r2=inner_radius);
    }

    difference() {    
        translate([br[0] - 5, br[1] - 5]) linear_extrude(height = 1.3) square(10);
        translate([br[0], br[1]]) cylinder(h=5,r1=inner_radius,r2=inner_radius);
    } 

    difference() {    
        translate([tr[0] - 5, tr[1] - 5]) linear_extrude(height = 1.3) square(10);
        translate([tr[0], tr[1]]) cylinder(h=5,r1=inner_radius,r2=inner_radius);
    }  
    
    difference() 
        {
            //base
            difference() {
                linear_extrude(height = 2.6) square([length,width]);
                //cutout center of holes
                translate([br[0], br[1]]) cylinder(h=5,r1=inner_radius,r2=inner_radius);
                translate([tl[0], tl[1]]) cylinder(h=5,r1=inner_radius,r2=inner_radius);
                translate([bl[0], bl[1]]) cylinder(h=5,r1=inner_radius,r2=inner_radius);
                translate([tr[0], tr[1]]) cylinder(h=5,r1=inner_radius,r2=inner_radius);
            }
            //cutouts to conserve material
            translate(c1) linear_extrude(height = 2.6) square([length / 2 - 20, width - 20]);
            translate(c2) linear_extrude(height = 2.6) square([length / 2 - 20, width - 20]);
        }
}

module screw_hole(loc) 
{
    translate([loc[0], loc[1]])
    
    difference() {
      cylinder(h=2.6,r1=outer_radius,r2=outer_radius);
      //cutout center of hole
      cylinder(h=5,r1=inner_radius,r2=inner_radius);
    }
  
}
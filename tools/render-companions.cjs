// Original black-and-white pixel artwork for Daylight.
// Development only: npm install --no-save @napi-rs/canvas, then node tools/render-companions.cjs.
// The Android app uses the generated PNG and has no JavaScript runtime dependency.
const {createCanvas}=require('@napi-rs/canvas');
const fs=require('fs');
const path=require('path');
const tile=createCanvas(128,48),ink=tile.getContext('2d');
let frame=0;
      const mask=()=>new Set();
      const cell=(m,x,y)=>m.add(`${Math.round(x)},${Math.round(y)}`);
      const lerp=(a,b,t)=>Math.round(a+(b-a)*Math.max(0,Math.min(1,t)));
      function box(m,x,y,w,h){for(let j=y;j<y+h;j++)for(let i=x;i<x+w;i++)cell(m,i,j);}
      function oval(m,cx,cy,rx,ry){
        for(let y=Math.floor(cy-ry);y<=cy+ry;y++)for(let x=Math.floor(cx-rx);x<=cx+rx;x++){
          if(((x-cx)/rx)**2+((y-cy)/ry)**2<=1)cell(m,x,y);
        }
      }
      function polygon(m,points){
        const xs=points.map(p=>p[0]),ys=points.map(p=>p[1]);
        for(let y=Math.min(...ys);y<=Math.max(...ys);y++)for(let x=Math.min(...xs);x<=Math.max(...xs);x++){
          let inside=false;
          for(let i=0,j=points.length-1;i<points.length;j=i++){
            const [xi,yi]=points[i],[xj,yj]=points[j];
            if((yi>y+.5)!==(yj>y+.5)&&x+.5<(xj-xi)*(y+.5-yi)/(yj-yi)+xi)inside=!inside;
          }
          if(inside)cell(m,x,y);
        }
      }
      function tube(m,points,r=1){
        for(let p=1;p<points.length;p++){
          const [a,b]=points[p-1],[c,d]=points[p],n=Math.max(Math.abs(c-a),Math.abs(d-b),1);
          for(let t=0;t<=n;t++){
            const x=Math.round(a+(c-a)*t/n),y=Math.round(b+(d-b)*t/n);
            for(let yy=-r;yy<=r;yy++)for(let xx=-r;xx<=r;xx++)cell(m,x+xx,y+yy);
          }
        }
      }
      function stamp(m,fill='#fff'){
        ink.fillStyle='#000';for(const p of m){const [x,y]=p.split(',').map(Number);ink.fillRect(x-1,y-1,3,3);}
        ink.fillStyle=fill;for(const p of m){const [x,y]=p.split(',').map(Number);ink.fillRect(x,y,1,1);}
      }
      function dot(x,y,w=1,h=1){ink.fillStyle='#000';ink.fillRect(x,y,w,h);}
      function catHead(cx,cy,closed=false){
        const m=mask();oval(m,cx,cy,8,6);
        polygon(m,[[cx-7,cy-3],[cx-7,cy-10],[cx-2,cy-5]]);
        polygon(m,[[cx+2,cy-5],[cx+7,cy-10],[cx+7,cy-3]]);stamp(m);
        dot(cx-6,cy-7,1,3);dot(cx-5,cy-6);dot(cx+5,cy-7,1,3);dot(cx+4,cy-6);
        if(closed){dot(cx-5,cy,3);dot(cx+3,cy,3);}else{dot(cx-4,cy-1,2,2);dot(cx+3,cy-1,2,2);}
        dot(cx,cy+2);dot(cx-1,cy+4);dot(cx+1,cy+4);dot(cx-9,cy+2,2);dot(cx+8,cy+2,2);
      }
      function catSleep(){
        const body=mask();oval(body,26,24,12,7);stamp(body);
        const head=mask();oval(head,17,25,7,5);
        polygon(head,[[11,23],[11,17],[16,21]]);polygon(head,[[20,21],[24,18],[24,25]]);stamp(head);
        dot(12,21);dot(22,22);dot(12,25,3);dot(19,25,3);dot(17,27);
        const tail=mask();tube(tail,[[36,24],[37,27],[34,30],[28,31],[23,30]],1);stamp(tail);
        dot(27,30,3);dot(23,29);dot(33,21,1,2);
      }
      function catSit(groom=false){
        const tail=mask();tube(tail,[[21,28],[16,29],[12,27],[12,24]],1);stamp(tail);dot(11,23,2,2);
        const body=mask();oval(body,26,25,7,6);stamp(body);
        const feet=mask();box(feet,20,29,5,2);box(feet,28,29,5,2);stamp(feet);
        catHead(26,16,groom||frame%8===7);dot(24,27,1,3);dot(28,27,1,3);
        if(groom){const paw=mask();tube(paw,[[30,26],[31,22],[29,20+(frame%3===0?1:0)]],1);stamp(paw);}
      }
      function catLazy(reach=0){
        const body=mask();oval(body,23,25,10,5);stamp(body);
        const tail=mask();tube(tail,[[15,26],[11,24],[10,20]],1);stamp(tail);
        catHead(33,23,reach===0);
        const paw=mask();box(paw,32,29,8+reach,2);stamp(paw);dot(37+reach,30);
      }
      function cat(kind,x=4,flip=false,reach=0){
        ink.save();ink.translate(Math.round(x),6);
        if(flip){ink.translate(48,0);ink.scale(-1,1);}
        if(kind==='sleep')catSleep();else if(kind==='lazy')catLazy(reach);else catSit(kind==='groom');
        ink.restore();
      }
      function dogHead(cx,cy,closed=false){
        const h=mask();oval(h,cx,cy,7,6);stamp(h);
        const muzzle=mask();oval(muzzle,cx-5,cy+4,5,3);stamp(muzzle);
        dot(cx-10,cy+2,3,2);
        if(closed)dot(cx-3,cy,3);else dot(cx-2,cy-1,2,2);
        dot(cx-3,cy+6,3);dot(cx,cy+5);
        const ear=mask();oval(ear,cx+6,cy+2,3,7);stamp(ear,'#000');
        ink.fillStyle='#fff';ink.fillRect(cx+6,cy+3,1,4);
      }
      function dogBody(kind){
        const walk=kind==='walk',down=kind==='down',sniff=kind==='sniff';
        const wag=frame%4<2?0:2;
        const tail=mask();tube(tail,down?[[35,27],[40,26],[42,23]]:[[36,22],[41,19],[43+wag,14]],1);stamp(tail);
        if(down){
          const body=mask();oval(body,28,27,10,4);stamp(body);
          dogHead(17,25,true);const paws=mask();box(paws,10,31,9,2);stamp(paws);return;
        }
        const shift=walk?(frame%4<2?-1:1):0;
        const backFeet=mask();box(backFeet,24+shift,26,4,5);box(backFeet,34-shift,25,4,6);stamp(backFeet);
        const body=mask();oval(body,29,23,9,6);stamp(body);
        const feet=mask();box(feet,22-shift,26,4,5);box(feet,32+shift,27,4,4);stamp(feet);
        dogHead(18,sniff?23:17+(walk?frame%2:0),kind==='sleepy');
        dot(29,20,2,2);
      }
      function dog(kind,x=72,flip=false,bounce=0){
        ink.save();ink.translate(Math.round(x),6+Math.round(bounce));
        if(flip){ink.translate(48,0);ink.scale(-1,1);}
        dogBody(kind);ink.restore();
      }
      function ball(x,y=36){
        const m=mask();oval(m,Math.round(x),y,3,3);stamp(m);
        dot(Math.round(x),y-2,1,2);dot(Math.round(x)-2,y,2,1);dot(Math.round(x)+1,y+1,1,2);
      }
      function book(turn=false){
        const left=mask();polygon(left,[[61,39],[49,35],[49,26],[61,29]]);stamp(left);
        const right=mask();polygon(right,[[61,39],[74,35],[74,26],[61,29]]);stamp(right);
        dot(61,29,1,10);dot(52,30,5);dot(52,33,5);dot(65,30,5);dot(65,33,5);
        if(turn){const page=mask();polygon(page,[[61,39],[57,33],[60,24],[65,29]]);stamp(page);}
      }
      function storyBall(){
        if(frame<4){cat('sleep',4,true);dog('idle',72);ball(76);return '猫睡得正香，狗有个好主意';}
        if(frame<16){const x=lerp(72,49,(frame-4)/11);cat('sleep',4,true);dog('walk',x);ball(x+4);return '狗把球慢慢推过来';}
        if(frame<20){cat('lazy',4,false,1);dog('idle',49);ball(53);return '喂，要不要一起玩？';}
        if(frame<24){cat('lazy',4,false,frame%2?4:2);dog('idle',49);ball(53);return '猫：好吧，就拨一下';}
        if(frame<32){
          const x=lerp(55,113,(frame-24)/7);
          cat('lazy',4,false,1);
          if(frame<29)dog('idle',49,false,frame>=26?-8:0);
          else dog('walk',lerp(49,69,(frame-29)/3),true,frame===29?-8:frame===30?-4:0);
          ball(x,36-(frame%3===1?2:0));return '球滚走了，狗赶紧追上';
        }
        if(frame<37){cat('lazy',4);dog('sniff',69,true);ball(113);return '找到了！';}
        cat('sleep',4,true);dog('down',72);ball(115);return '猫翻个身继续睡，狗守着它的球';
      }
      function storyAlone(){
        cat(frame<16?'groom':'sleep',4,frame>=16);
        if(frame<10){dog('sniff',72);ball(63);return '猫洗洗脸，狗闻闻地板';}
        if(frame<20){dog('idle',72,frame%8<4);ball(63);return '你忙你的，我看看我的尾巴';}
        dog('down',72);ball(63);return '各自发呆，也很好';
      }
      function storyPlay(){
        if(frame<8){cat('sit',8);dog('walk',lerp(72,42,frame/7));return '狗又来找猫玩了';}
        if(frame<12){cat('lazy',8,false,2);dog('sniff',42);return '先凑近一点点';}
        if(frame<18){cat('lazy',8,false,frame%2?5:1);dog('sniff',frame%2?47:44);return '猫轻轻拍两下，狗躲一下';}
        if(frame<24){cat('sit',8);dog('walk',lerp(47,64,(frame-18)/5),false,frame%2?-2:0);return '闹完了，又和好了';}
        cat('groom',8);dog('idle',66);return '猫整理毛，狗摇摇尾巴';
      }
      function storyRead(){
        cat('sit',17);
        dog(frame>=22?'sleepy':'idle',64);
        book(frame===8||frame===9||frame===17||frame===18);
        if(frame<8)return '一起看两页书';
        if(frame<20)return '猫翻一页，狗认真听';
        return '猫还在看，狗已经有点困了';
      }

const scenes=[['rest',1,()=>{cat('sleep',4,true);dog('down',72);ball(115);}],
              ['alone',29,storyAlone],['ball',45,storyBall],['play',33,storyPlay],['read',33,storyRead]];
const sheet=createCanvas(1024,864),out=sheet.getContext('2d');
out.fillStyle='#fff';out.fillRect(0,0,sheet.width,sheet.height);
let index=0;
for(const [name,count,draw] of scenes){
  for(frame=0;frame<count;frame++){
    ink.setTransform(1,0,0,1,0,0);ink.fillStyle='#fff';ink.fillRect(0,0,128,48);
    draw();out.drawImage(tile,(index%8)*128,Math.floor(index/8)*48);index++;
  }
}
const target=path.resolve(__dirname,'../app/src/main/res/drawable-nodpi/companion_sprites.png');
fs.mkdirSync(path.dirname(target),{recursive:true});fs.writeFileSync(target,sheet.toBuffer('image/png'));
console.log(JSON.stringify({frames:index,width:sheet.width,height:sheet.height,bytes:fs.statSync(target).size}));
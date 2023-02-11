"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[369],{3905:(e,t,n)=>{n.d(t,{Zo:()=>c,kt:()=>d});var a=n(7294);function r(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function o(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);t&&(a=a.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,a)}return n}function l(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?o(Object(n),!0).forEach((function(t){r(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):o(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function i(e,t){if(null==e)return{};var n,a,r=function(e,t){if(null==e)return{};var n,a,r={},o=Object.keys(e);for(a=0;a<o.length;a++)n=o[a],t.indexOf(n)>=0||(r[n]=e[n]);return r}(e,t);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(a=0;a<o.length;a++)n=o[a],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(r[n]=e[n])}return r}var m=a.createContext({}),s=function(e){var t=a.useContext(m),n=t;return e&&(n="function"==typeof e?e(t):l(l({},t),e)),n},c=function(e){var t=s(e.components);return a.createElement(m.Provider,{value:t},e.children)},u="mdxType",p={inlineCode:"code",wrapper:function(e){var t=e.children;return a.createElement(a.Fragment,{},t)}},k=a.forwardRef((function(e,t){var n=e.components,r=e.mdxType,o=e.originalType,m=e.parentName,c=i(e,["components","mdxType","originalType","parentName"]),u=s(n),k=r,d=u["".concat(m,".").concat(k)]||u[k]||p[k]||o;return n?a.createElement(d,l(l({ref:t},c),{},{components:n})):a.createElement(d,l({ref:t},c))}));function d(e,t){var n=arguments,r=t&&t.mdxType;if("string"==typeof e||r){var o=n.length,l=new Array(o);l[0]=k;var i={};for(var m in t)hasOwnProperty.call(t,m)&&(i[m]=t[m]);i.originalType=e,i[u]="string"==typeof e?e:r,l[1]=i;for(var s=2;s<o;s++)l[s]=n[s];return a.createElement.apply(null,l)}return a.createElement.apply(null,n)}k.displayName="MDXCreateElement"},9139:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>m,contentTitle:()=>l,default:()=>p,frontMatter:()=>o,metadata:()=>i,toc:()=>s});var a=n(7462),r=(n(7294),n(3905));const o={},l="Issue Management Commands",i={unversionedId:"commands/available_commands/issue_management",id:"commands/available_commands/issue_management",title:"Issue Management Commands",description:"move",source:"@site/docs/commands/available_commands/issue_management.md",sourceDirName:"commands/available_commands",slug:"/commands/available_commands/issue_management",permalink:"/actionable/docs/commands/available_commands/issue_management",draft:!1,editUrl:"https://github.com/MinecraftForge/Actionable/tree/v1/docs/docs/commands/available_commands/issue_management.md",tags:[],version:"current",frontMatter:{},sidebar:"tutorialSidebar",previous:{title:"Commands",permalink:"/actionable/docs/commands/"},next:{title:"Misc Commands",permalink:"/actionable/docs/commands/available_commands/misc"}},m={},s=[{value:"<code>move</code>",id:"move",level:2},{value:"<code>title</code>",id:"title",level:2},{value:"<code>lock</code>",id:"lock",level:2},{value:"<code>clock</code>",id:"clock",level:2}],c={toc:s},u="wrapper";function p(e){let{components:t,...n}=e;return(0,r.kt)(u,(0,a.Z)({},c,n,{components:t,mdxType:"MDXLayout"}),(0,r.kt)("h1",{id:"issue-management-commands"},"Issue Management Commands"),(0,r.kt)("h2",{id:"move"},(0,r.kt)("inlineCode",{parentName:"h2"},"move")),(0,r.kt)("p",null,"Moves the issue this command is run in to another repository.  "),(0,r.kt)("blockquote",null,(0,r.kt)("p",{parentName:"blockquote"},"Aliases: ",(0,r.kt)("inlineCode",{parentName:"p"},"transfer"),(0,r.kt)("br",{parentName:"p"}),"\n","Syntax: ",(0,r.kt)("inlineCode",{parentName:"p"},"/move <repo>"),"  ")),(0,r.kt)("p",null,"Parameters:  "),(0,r.kt)("ul",null,(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("inlineCode",{parentName:"li"},"repo")," (GHRepository): the repository to move the issue to  ")),(0,r.kt)("blockquote",null,(0,r.kt)("p",{parentName:"blockquote"},"Requirements: ",(0,r.kt)("a",{parentName:"p",href:"../requirements#can_manage_issue"},(0,r.kt)("inlineCode",{parentName:"a"},"can_manage_issue")),", ",(0,r.kt)("a",{parentName:"p",href:"../requirements#in_pull_request"},(0,r.kt)("inlineCode",{parentName:"a"},"in_pull_request")),"  ")),(0,r.kt)("h2",{id:"title"},(0,r.kt)("inlineCode",{parentName:"h2"},"title")),(0,r.kt)("p",null,"Set the title of the issue.  "),(0,r.kt)("blockquote",null,(0,r.kt)("p",{parentName:"blockquote"},"Syntax: ",(0,r.kt)("inlineCode",{parentName:"p"},"/title <title>"),"  ")),(0,r.kt)("p",null,"Parameters:  "),(0,r.kt)("ul",null,(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("inlineCode",{parentName:"li"},"title")," (String): the new title of the issue  ")),(0,r.kt)("blockquote",null,(0,r.kt)("p",{parentName:"blockquote"},"Requirements: ",(0,r.kt)("a",{parentName:"p",href:"../requirements#can_manage_issue"},(0,r.kt)("inlineCode",{parentName:"a"},"can_manage_issue")),"  ")),(0,r.kt)("h2",{id:"lock"},(0,r.kt)("inlineCode",{parentName:"h2"},"lock")),(0,r.kt)("p",null,"Lock the issue.  "),(0,r.kt)("blockquote",null,(0,r.kt)("p",{parentName:"blockquote"},"Syntax: ",(0,r.kt)("inlineCode",{parentName:"p"},"/lock [reason]"),"  ")),(0,r.kt)("p",null,"Parameters:  "),(0,r.kt)("ul",null,(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("inlineCode",{parentName:"li"},"reason")," (",(0,r.kt)("a",{parentName:"li",href:"../arguments#lockreason"},"LockReason"),"): the reason for locking the issue  ")),(0,r.kt)("blockquote",null,(0,r.kt)("p",{parentName:"blockquote"},"Requirements: ",(0,r.kt)("a",{parentName:"p",href:"../requirements#can_manage_issue"},(0,r.kt)("inlineCode",{parentName:"a"},"can_manage_issue")),"  ")),(0,r.kt)("h2",{id:"clock"},(0,r.kt)("inlineCode",{parentName:"h2"},"clock")),(0,r.kt)("p",null,"Lock and close the issue.  "),(0,r.kt)("blockquote",null,(0,r.kt)("p",{parentName:"blockquote"},"Syntax: ",(0,r.kt)("inlineCode",{parentName:"p"},"/clock [reason]"),"  ")),(0,r.kt)("p",null,"Parameters:  "),(0,r.kt)("ul",null,(0,r.kt)("li",{parentName:"ul"},(0,r.kt)("inlineCode",{parentName:"li"},"reason")," (",(0,r.kt)("a",{parentName:"li",href:"../arguments#lockreason"},"LockReason"),"): the reason for locking the issue. If ",(0,r.kt)("inlineCode",{parentName:"li"},"resolved"),", the close reason will be ",(0,r.kt)("inlineCode",{parentName:"li"},"completed"),", otherwise it will be ",(0,r.kt)("inlineCode",{parentName:"li"},"not_planned"),"  ")),(0,r.kt)("blockquote",null,(0,r.kt)("p",{parentName:"blockquote"},"Requirements: ",(0,r.kt)("a",{parentName:"p",href:"../requirements#can_manage_issue"},(0,r.kt)("inlineCode",{parentName:"a"},"can_manage_issue")))))}p.isMDXComponent=!0}}]);
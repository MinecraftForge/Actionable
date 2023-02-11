"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[698],{3905:(e,t,n)=>{n.d(t,{Zo:()=>u,kt:()=>f});var r=n(7294);function a(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function o(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);t&&(r=r.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,r)}return n}function i(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?o(Object(n),!0).forEach((function(t){a(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):o(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function s(e,t){if(null==e)return{};var n,r,a=function(e,t){if(null==e)return{};var n,r,a={},o=Object.keys(e);for(r=0;r<o.length;r++)n=o[r],t.indexOf(n)>=0||(a[n]=e[n]);return a}(e,t);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(r=0;r<o.length;r++)n=o[r],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(a[n]=e[n])}return a}var m=r.createContext({}),c=function(e){var t=r.useContext(m),n=t;return e&&(n="function"==typeof e?e(t):i(i({},t),e)),n},u=function(e){var t=c(e.components);return r.createElement(m.Provider,{value:t},e.children)},l="mdxType",p={inlineCode:"code",wrapper:function(e){var t=e.children;return r.createElement(r.Fragment,{},t)}},d=r.forwardRef((function(e,t){var n=e.components,a=e.mdxType,o=e.originalType,m=e.parentName,u=s(e,["components","mdxType","originalType","parentName"]),l=c(n),d=a,f=l["".concat(m,".").concat(d)]||l[d]||p[d]||o;return n?r.createElement(f,i(i({ref:t},u),{},{components:n})):r.createElement(f,i({ref:t},u))}));function f(e,t){var n=arguments,a=t&&t.mdxType;if("string"==typeof e||a){var o=n.length,i=new Array(o);i[0]=d;var s={};for(var m in t)hasOwnProperty.call(t,m)&&(s[m]=t[m]);s.originalType=e,s[l]="string"==typeof e?e:a,i[1]=s;for(var c=2;c<o;c++)i[c]=n[c];return r.createElement.apply(null,i)}return r.createElement.apply(null,n)}d.displayName="MDXCreateElement"},380:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>m,contentTitle:()=>i,default:()=>p,frontMatter:()=>o,metadata:()=>s,toc:()=>c});var r=n(7462),a=(n(7294),n(3905));const o={},i="Command Requirements",s={unversionedId:"commands/requirements",id:"commands/requirements",title:"Command Requirements",description:"Below you can find a list of requirements a command may have. A command with requirements will only run when it meets all its requirements.",source:"@site/docs/commands/requirements.md",sourceDirName:"commands",slug:"/commands/requirements",permalink:"/actionable/docs/commands/requirements",draft:!1,editUrl:"https://github.com/MinecraftForge/Actionable/tree/v1/docs/docs/commands/requirements.md",tags:[],version:"current",frontMatter:{},sidebar:"tutorialSidebar",previous:{title:"Arguments",permalink:"/actionable/docs/commands/arguments"}},m={},c=[{value:"<code>has_write_perms</code>",id:"has_write_perms",level:3},{value:"<code>can_manage_issue</code>",id:"can_manage_issue",level:3},{value:"<code>in_pull_request</code>",id:"in_pull_request",level:3}],u={toc:c},l="wrapper";function p(e){let{components:t,...n}=e;return(0,a.kt)(l,(0,r.Z)({},u,n,{components:t,mdxType:"MDXLayout"}),(0,a.kt)("h1",{id:"command-requirements"},"Command Requirements"),(0,a.kt)("p",null,"Below you can find a list of requirements a command may have. A command with requirements will only run when it meets all its requirements.  "),(0,a.kt)("h3",{id:"has_write_perms"},(0,a.kt)("inlineCode",{parentName:"h3"},"has_write_perms")),(0,a.kt)("p",null,"The user running the command must have writer permissions to the repository the command is run in.  "),(0,a.kt)("h3",{id:"can_manage_issue"},(0,a.kt)("inlineCode",{parentName:"h3"},"can_manage_issue")),(0,a.kt)("p",null,"The user running the command must either be part of the triage team or have write permissions to the repository the command is run in.  "),(0,a.kt)("h3",{id:"in_pull_request"},(0,a.kt)("inlineCode",{parentName:"h3"},"in_pull_request")),(0,a.kt)("p",null,"The command may only be run in a pull request, and NOT an issue."))}p.isMDXComponent=!0}}]);
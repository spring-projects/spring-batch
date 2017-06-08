$('.toggleWidget').each(function(index) {
   if(index > 0) {
       $(this).hide();
   }
});

$(function(){
    if(!window.widgetInitalized) {
        window.widgetInitalized = true;
        new Spring.ProjectDocumentationWidget();
    }
});

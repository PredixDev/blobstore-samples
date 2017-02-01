   $(function() {
       $('.delete-blob-link').click(function(event) {
       $.ajax({
           url:'/v1/blob/' + $(event.target).attr('data-blob-id'),
           type: 'DELETE',
           success: function(){
                alert('Successfully deleted file: ' + $(event.target).attr('data-blob-id'));
           },
           error : function(request) {
                alert('Error deleting file: ' + $(event.target).attr('data-blob-id'));
                console.log(request.responseText);
                location.reload();
           }
       }).then(function(){
                   $('[id="' + $(event.target).attr('data-blob-id')+'"]').remove();
           });
       });

       $('.upload-blob-btn').click(function(event) {
       event.stopPropagation();
       event.preventDefault();
       var files = document.getElementById('image').files
           var fd = new FormData();
           fd.append("file", files[0], files[0].name);

           $.ajax({
               url: '/v1/blob',
               data: fd,
               processData: false,
               contentType: false,
               type: 'POST',
               success: function(){
                    alert('Successfully uploaded file: ' + files[0].name);
               },
               error : function(request) {
                    alert('Error uploading file: ' + files[0].name);
                    console.log(request.responseText);
               }
           }).then(function(){
               location.reload();
           });
       });
   });

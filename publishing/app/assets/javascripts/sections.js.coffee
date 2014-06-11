# on load run:
$ ->
  $('.delete_panel').bind 'click', (event) ->
    if confirm('Are you sure you want to delete this?')
      deleteItem = $(event.currentTarget).data('item')
      star = $(event.currentTarget).data('star')
      url = $(event.currentTarget).data('url')
      $.ajax url,
          type: 'DELETE'
          dataType: 'json'
          success: (data, textStatus, jqXHR) ->
            $(deleteItem).remove()
            $('#badge_' + star).html(data.count)
            refreshPreview(star)

  $('.save').bind 'click', (event) ->
    form =$("#section-form")
    url = form.context.URL
    $("#edit-time").html('<img src="/assets/spinner.gif" alt="Wait" />')
    $.ajax url,
        type: 'POST'
        dataType: 'json'
        data: form.serialize()
        success: (data, textStatus, jqXHR) ->
          $("#edit-time").html(data.updated_at)
          refreshPreview(1)
          refreshPreview(2)
          refreshPreview(3)



#################################################################################
#
# saveSection = (url) ->
#   values = $('.editable').editable('getValue')
#   $("#edit-time").html('<img src="/assets/spinner.gif" alt="Wait" />')
#   $.ajax url,
#       type: 'PUT'
#       dataType: 'json'
#       data: { section: values }
#       success: (data, textStatus, jqXHR) ->
#         $("#edit-time").html(data.updated_at)

refreshPreview = (star) ->
  $('#preview_' + star).attr("src", $('#preview_' + star).attr("src"))

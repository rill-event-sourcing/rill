# on load run:
$ ->
  $('.save').bind 'click', (event) ->
    url = $(event.currentTarget).data('url')
    saveSection(url)
    refreshPreview()

################################################################################

saveSection = (url) ->
  values = $('.editable').editable('getValue')
  $("#edit-time").html('<img src="/assets/spinner.gif" alt="Wait" />')
  $.ajax url,
      type: 'PUT'
      dataType: 'json'
      data: { section: values }
      success: (data, textStatus, jqXHR) ->
        $("#edit-time").html(data.updated_at)

refreshPreview = ->
  $('#preview').attr("src", $('#preview').attr("src"))

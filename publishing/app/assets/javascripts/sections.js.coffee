# on load run:
$ ->
  $('.save').bind 'click', (event) ->
    url = $(event.currentTarget).data('url')
    saveSection(url)
    refreshPreview()

  # $('#subsection-tabs a').bind 'click', (event) ->
  #   url = $(event.currentTarget).data('url')
  #   $("#subsection-list").html('<img src="/assets/spinner.gif" alt="Wait" />')
  #   $("#subsection-list").load url, ->
  #     setSaveBtnHandler()

  $.fn.editable.defaults.mode = 'inline'
  $.fn.editable.defaults.showbuttons = false
  $.fn.editable.defaults.clear = false
  $.fn.editable.defaults.onblur = 'submit'
  $('.editable').editable()

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

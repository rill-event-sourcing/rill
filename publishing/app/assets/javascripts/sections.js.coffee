# on load run:
$ ->
  $('#subsection-tabs a').bind 'click', (event) ->
    url = $(event.currentTarget).data('url')
    $("#subsection-list").html('<img src="/assets/spinner.gif" alt="Wait" />')
    $("#subsection-list").load url, ->
      refreshPreview()
      setSave()
  firstTab()
  refreshPreview()

################################################################################

firstTab = ->
  $("#subsection-list").load firstTabUrl, ->
    setSave()

setSave = ->
  $('.save').bind 'click', (event) ->
    refreshPreview()

refreshPreview = ->
  $('#preview').attr("src", $('#preview').attr("src"))

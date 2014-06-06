# on load run:
$ ->
  $('#subsection-tabs a').bind 'click', (event) ->
    url = $(event.currentTarget).data('url')
    $("#subsection-list").html('<img src="/assets/spinner.gif" alt="Wait" />')
    $("#subsection-list").load url, ->
      setSaveBtnHandler()

  clickFirstTab()

  $.fn.editable.defaults.mode = 'inline'
  $.fn.editable.defaults.showbuttons = false
  $.fn.editable.defaults.clear = false
  $.fn.editable.defaults.onblur = 'submit'
  $('.editable').editable()

################################################################################

clickFirstTab = ->
  firstTab = $('#subsection-tabs a').first()
  if firstTab
    firstTab.click()

setSaveBtnHandler = ->
  $('.save').bind 'click', (event) ->
    refreshPreview()

refreshPreview = ->
  $('#preview').attr("src", $('#preview').attr("src"))

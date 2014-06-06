# Place all the behaviors and hooks related to the matching controller here.
# All this logic will automatically be available in application.js.
# You can use CoffeeScript in this file: http://coffeescript.org/


updatePreview = ->
  $.get previewUrl, (data) ->
    $("#preview").contents().find('html').html(data)


firstTab = ->
  $("#subsection-list").load(firstTabUrl)


$ ->
  $('#subsection-tabs a').bind 'click', (event) ->
    url = $(event.currentTarget).data('url')
    $("#subsection-list").html('<img src="/assets/spinner.gif" alt="Wait" />')
    $("#subsection-list").load url, ->
      updatePreview()

  firstTab()
  updatePreview()

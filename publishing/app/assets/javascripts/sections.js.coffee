# Place all the behaviors and hooks related to the matching controller here.
# All this logic will automatically be available in application.js.
# You can use CoffeeScript in this file: http://coffeescript.org/

# oldVals = []
#
# setPreview = (subsection_id) ->
#   $("subsection_preview_" + subsection_id).contents().find('html').html(oldVals[subsection_id]);
#
# syncPreview = (subsection_id) ->
#   # alert 'ok'
#   console.log $("subsection_editor_" + subsection_id)
#   oldVals[subsection_id] = $("subsection_editor_" + subsection_id).text()
#   alert oldVals

  # setPreview(subsection_id)
  # $("subsection_editor_" + subsection_id).bind "change keyup paste", (event) ->
  #   currentVal = $(this).val()
  #   if(currentVal == oldVals[subsection_id])
  #     return
  #   oldVals[subsection_id] = currentVal
  #   setPreview(subsection_id)


firstTab = ->
  $("#subsection-list").load(firstTabUrl)


$ ->
  $('#subsection-tabs a').bind 'click', (event) ->
    url = $(event.currentTarget).data('url')
    $("#subsection-list").load(url)

  firstTab()

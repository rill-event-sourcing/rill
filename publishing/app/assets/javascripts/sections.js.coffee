# Place all the behaviors and hooks related to the matching controller here.
# All this logic will automatically be available in application.js.
# You can use CoffeeScript in this file: http://coffeescript.org/

$ ->
  $('#subsection-tabs a').bind 'click', (event, data) ->
    event.stopPropagation()
    clickedEl = $(event.target)
    console.log(clickedEl)
    $.ajax '/chapters',
      type: 'GET'
      dataType: 'html'
      error: (jqXHR, textStatus, errorThrown) ->
        console.log textStatus
      success: (res) ->
        #console.log "Card name: " + res

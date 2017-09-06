$(function($) {
		$.expr[":"].icontains = jQuery.expr.createPseudo(function(arg) {
		    return function( elem ) {
		        return $(elem).text().toUpperCase().indexOf(arg.toUpperCase()) >= 0;
		    };
		});
		
		$('.dropdown-menu.m2m-list').click(function(e) { e.stopPropagation(); });
});
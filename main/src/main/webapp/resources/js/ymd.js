$(function($) {
		$.expr[":"].icontains = jQuery.expr.createPseudo(function(arg) {
		    return function( elem ) {
		        return $(elem).text().toUpperCase().indexOf(arg.toUpperCase()) >= 0;
		    };
		});
		
		$('.dropdown-menu.m2m-list').click(function(e) { e.stopPropagation(); });

		$('#signin-modal').on('shown.bs.modal', function(e) {
			$('#signin\\:username').focus();
		});
		
		$('#signup-modal').on('shown.bs.modal', function(e) {
			$('#signup\\:username').focus();
		});
});